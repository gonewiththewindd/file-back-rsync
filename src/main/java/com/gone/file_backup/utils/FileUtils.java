package com.gone.file_backup.utils;

import com.gone.file_backup.file.FileBlock;
import com.gone.file_backup.file.RollingChecksum;
import com.gone.file_backup.file.SearchFileBlock;
import com.gone.file_backup.file.reconstruct.MatchedFileBlock;
import com.gone.file_backup.file.reconstruct.ModifyByteSerial;
import com.gone.file_backup.file.reconstruct.ReconstructFileBlock;
import com.gone.file_backup.model.SliceFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.RandomAccessFileMode;
import org.apache.tomcat.util.buf.ByteBufferUtils;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class FileUtils {

    private static final Integer BLOCK_SIZE = 4096;

    /**
     * 文件块匹配搜索
     *
     * @param deltaPath
     * @param sliceFile
     * @return
     */
    public static List<ReconstructFileBlock> doSearch(Path deltaPath, SliceFile sliceFile) {
        long startTime = System.currentTimeMillis();
        List<ReconstructFileBlock> reconstructList = new ArrayList<>();
        RollingChecksum lastChecksum = null;
        int modifyBegin = -1;
        ByteBuffer changeBuffer = ByteBuffer.allocate(1024 * 1024);
        // 有没有可能多线程并非搜索，难点在于线程负责的区域不可重叠，
        // 这样可能出现的情况就是线程和线程相邻的区域割裂，导致潜在的命中块降低，最坏的情况就是N个块都属于命中块
        long length = deltaPath.toFile().length();
        int BLOCK_SIZE = sliceFile.getBlockSize();
        int bufferSize = BLOCK_SIZE * 1024;
        byte[] buffer = loadBytes(deltaPath, 0, bufferSize);
        int bufferOffset = 0;// 当前缓冲区在文件中的偏移量
        int current = 0;// 当前文件的比较字节下标, buffer的索引下标 =  current - bufferOffset
        Map<Integer, List<FileBlock>> checksumMap = sliceFile.getFileBlockList()
                .stream()
                .collect(Collectors.groupingBy(block -> block.getChecksum().getS()));
        for (; current < length - BLOCK_SIZE; ) {
            int bufferIndex = current - bufferOffset;
            if (bufferIndex > bufferSize - BLOCK_SIZE) {
                buffer = loadBytes(deltaPath, current - 1, bufferSize);
                bufferOffset = current - 1;
            }
            SearchFileBlock searchFileBlock = searchFileBlock(buffer, current, bufferOffset, BLOCK_SIZE, checksumMap, lastChecksum);
            if (searchFileBlock.isMatched()) {
                // 之前缓存的未匹配的连续修改字节先处理
                if (changeBuffer.position() > 0) {
                    ReconstructFileBlock reconstructFileBlock = generateModifyByteSerial(changeBuffer, modifyBegin, current);
                    reconstructList.add(reconstructFileBlock);
                    modifyBegin = -1;
                }
                // 处理匹配的块
                reconstructList.add(searchFileBlock.getMatchedFileBlock());
                current += BLOCK_SIZE;
                lastChecksum = null;
//                matchedCnt.getAndIncrement();
            } else {
                // 合并连续字节修改序列
                if (modifyBegin < 0) {
                    modifyBegin = current; // 记录连续修改字节起始位置
                }
                if (changeBuffer.position() >= changeBuffer.capacity()) { // 扩容
                    changeBuffer = ByteBufferUtils.expand(changeBuffer, changeBuffer.capacity() * 2);
                }
                changeBuffer.put(buffer[current - bufferOffset]);
                lastChecksum = searchFileBlock.getLastChecksum();
                current++;
            }
        }
        if (current < length) {
            if (modifyBegin < 0) {//文件最后不存在连续修改内容，修改起始位置重置为当前位置
                modifyBegin = current;
            }
            byte[] rest = loadBytes(deltaPath, current, (int) length - current);
            changeBuffer.put(rest);
        }
        // 可能存在最后未匹配未处理的缓存
        if (changeBuffer.position() > 0) {
            ReconstructFileBlock reconstructFileBlock = generateModifyByteSerial(changeBuffer, modifyBegin, (int) length);
            reconstructList.add(reconstructFileBlock);
        }

        log.info("search from byte {} to {}, end in {} ms", 0, length, System.currentTimeMillis() - startTime);
        return reconstructList;
    }

    private static byte[] loadBytes(Path deltaPath, int current, int length) {
        byte[] buffer = new byte[length];
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(deltaPath))) {
            bufferedInputStream.skip(current);
            bufferedInputStream.read(buffer, 0, length);
            return buffer;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static SearchFileBlock searchFileBlock(byte[] bytes,
                                                   int from,
                                                   int bufferOffset,
                                                   int BLOCK_SIZE,
                                                   Map<Integer, List<FileBlock>> checksumMap,
                                                   RollingChecksum lastChecksum) {
        int to = from + BLOCK_SIZE;
        RollingChecksum checksum;
        if (Objects.isNull(lastChecksum)) { // 命中匹配块以后重新计算校验和
            checksum = SignatureUtils.checksum(bytes, from, BLOCK_SIZE, bufferOffset);
        } else {// 其他情况通过滚动校验和计算下一个块校验和
            checksum = SignatureUtils.nextBlockCheckSum(bytes, to - 1, from - 1, bufferOffset, lastChecksum);
        }
//        Map<Integer, List<FileBlock>> checksumMap = basisFile.getFileBlockMap();
        SearchFileBlock searchFileBlock = new SearchFileBlock().setLastChecksum(checksum);
        if (checksumMap.containsKey(checksum.getS())) {
            // 滚动校验和匹配的情况下，校验md5是否匹配
            byte[] targetBytes = Arrays.copyOfRange(bytes, from - bufferOffset, to - bufferOffset);
            String targetMd5 = DigestUtils.md5DigestAsHex(targetBytes);
            List<FileBlock> fileBlocks = checksumMap.get(checksum.getS());
            Optional<FileBlock> targetFileBlock = fileBlocks.stream()
                    .filter(fileBlock -> /*quickMatch(fileBlock, targetBytes) &&*/ fileBlock.getMd5().equalsIgnoreCase(targetMd5))
                    .findFirst();
            if (targetFileBlock.isPresent()) {
                MatchedFileBlock matchedFileBlock = new MatchedFileBlock()
                        .setId(targetFileBlock.get().getId())
                        .setBlockChecksum(checksum);
                matchedFileBlock.setFrom(from);
                matchedFileBlock.setTo(to);

                searchFileBlock.setMatchedFileBlock(matchedFileBlock);
                searchFileBlock.setLastChecksum(null);
                return searchFileBlock;
            }
        }
        return searchFileBlock;
    }

    private static ReconstructFileBlock generateModifyByteSerial(ByteBuffer byteBuffer, int from, int to) {

        byte[] bytes = new byte[byteBuffer.position()];
        byteBuffer.rewind();
        byteBuffer.get(bytes);
        byteBuffer.clear();

        return new ModifyByteSerial().setContent(bytes).setFrom(from).setTo(to);
    }

    public static Path reconstructFile(String deltaFilePath, List<FileBlock> fileBlocks, Set<ReconstructFileBlock> reconstructFileBlocks) {
        Map<String, FileBlock> fileBlockMap = fileBlocks.stream()
                .collect(Collectors.toMap(FileBlock::getId, v -> v));
        List<ReconstructFileBlock> sortedReconstructFileBlocks = reconstructFileBlocks.stream()
                .sorted(Comparator.comparing(ReconstructFileBlock::getFrom))
                .collect(Collectors.toList());

        String extension = FilenameUtils.getExtension(deltaFilePath);
        String filename = UUID.randomUUID().toString();
        try {
            Path tempFile = Files.createTempFile(filename, "." + extension);
            try (OutputStream bufferedWriter = Files.newOutputStream(tempFile);
                 RandomAccessFile randomAccessFile = new RandomAccessFile(new File(deltaFilePath), RandomAccessFileMode.READ_ONLY.toString())) {
                for (ReconstructFileBlock fb : sortedReconstructFileBlocks) {
                    if (fb instanceof ModifyByteSerial) {
                        bufferedWriter.write(((ModifyByteSerial) fb).getContent());
                    } else {
                        MatchedFileBlock mfb = (MatchedFileBlock) fb;
                        FileBlock fileBlock = fileBlockMap.get(mfb.getId());
                        randomAccessFile.seek(fileBlock.getFrom());

                        byte[] bytes = new byte[fileBlock.getTo() - fileBlock.getFrom()];
                        int read = randomAccessFile.read(bytes);
                        bufferedWriter.write(bytes, 0, read);
                    }
                }
            }
            return tempFile.toAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static SliceFile slice(String fid, String path) {
        long start = System.currentTimeMillis();
        Path path1 = Paths.get(path);
        File file = path1.toFile();
        long length = file.length();
        int blockSize = BLOCK_SIZE;
        List<FileBlock> fileBlocks = new ArrayList<>((int) (length / blockSize + 1));
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
            for (int from = 0, to, index = 0; from < length; from += blockSize) {
                to = from + blockSize;
                if (to > length) {
                    to = (int) length;
                }
                byte[] content = new byte[to - from];
                bufferedInputStream.read(content, 0, blockSize);
                FileBlock fileBlock = new FileBlock()
                        .setId(UUIDUtils.randomUUID())
                        .setIndex(index++);
                fileBlock.setFrom(from);
                fileBlock.setTo(to);
                fileBlock.setChecksum(SignatureUtils.checksum(content, from, content.length, 0));
                fileBlock.setMd5(DigestUtils.md5DigestAsHex(content));
                fileBlock.setContent(content);
                fileBlocks.add(fileBlock);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        log.info("load and init file {} end in {} ms", path, System.currentTimeMillis() - start);
        return new SliceFile().setBlockSize(blockSize).setFileBlockList(fileBlocks);
    }

}
