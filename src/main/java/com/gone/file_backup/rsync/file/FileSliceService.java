package com.gone.file_backup.rsync.file;

import com.gone.file_backup.file.FileBlock;
import com.gone.file_backup.file.FileItem;
import com.gone.file_backup.rsync.utils.SignatureUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FileSliceService {

    @Value("${com.bdc.file.backup.slice.block-size:4096}")
    private Integer BLOCK_SIZE;

    public FileItem slice(String fid, String path) {
        long start = System.currentTimeMillis();
        Path path1 = Paths.get(path);
        File file = path1.toFile();
        long length = file.length();
        List<FileBlock> fileBlocks = new ArrayList<>((int) (length / BLOCK_SIZE + 1));
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(file))) {
            for (int from = 0, to, index = 0; from < length; from += BLOCK_SIZE) {
                to = from + BLOCK_SIZE;
                if (to > length) {
                    to = (int) length;
                }
                byte[] content = new byte[to - from];
                bufferedInputStream.read(content, 0, BLOCK_SIZE);
                FileBlock fileBlock = new FileBlock()
                        .setId(fid)
                        .setIndex(index);
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

        Map<Integer, List<FileBlock>> fileBlockMap = fileBlocks.stream().collect(Collectors.groupingBy(v -> v.getChecksum().getS()));
        log.info("load and init file {} end in {} ms", path, System.currentTimeMillis() - start);

        return new FileItem()
                .setPath(path1)
                .setFileBlockMap(fileBlockMap);

    }


}
