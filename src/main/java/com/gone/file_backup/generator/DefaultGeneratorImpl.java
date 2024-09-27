package com.gone.file_backup.generator;

import com.gone.file_backup.file.reconstruct.ReconstructFileBlock;
import com.gone.file_backup.model.FileMetaInfo;
import com.gone.file_backup.model.SliceFile;
import com.gone.file_backup.sender.Sender;
import com.gone.file_backup.utils.FileUtils;
import com.gone.file_backup.utils.SignatureUtils;
import com.gone.file_backup.utils.UUIDUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DefaultGeneratorImpl implements Generator {

    private String ip;
    private int port;
    private File directory;
    private String destDirectory;
    private Map<String, FileMetaInfo> latestFileMetaInfoMap = new HashMap<>();
    private Sender sender;

    public DefaultGeneratorImpl(String ip, int port, String directory, String destDirectory, Sender sender) {
        this.directory = Paths.get(directory).toFile();
        this.destDirectory = destDirectory;
        this.sender = sender;
        if (!this.directory.isDirectory()) {
            throw new IllegalArgumentException(String.format("directory '%s' is not a directory", directory));
        }
    }

    @Override
    public List<FileMetaInfo> generateFileList() {
        log.info("[Generator]Generate file list...");
        List<FileMetaInfo> fileMetaInfos = doGenerateFileList(this.directory, true, true);
        fileMetaInfos.forEach(f -> {
            // 设置相对路径
            int parentIndex = f.getAbsolutePath().indexOf(this.directory.getAbsolutePath());
            String relativeFilename = f.getAbsolutePath().substring(parentIndex + this.directory.getAbsolutePath().length());
//            String relativePath = relativeFilename.replace(f.getName(), "");
            f.setDestPath(this.destDirectory.concat(relativeFilename));
        });
        this.latestFileMetaInfoMap = fileMetaInfos.stream()
                .collect(Collectors.toMap(FileMetaInfo::getAbsolutePath, v -> v));
        return fileMetaInfos;
    }

    private List<FileMetaInfo> doGenerateFileList(File dir, boolean initFid, boolean computeMD5) {
        List<FileMetaInfo> fileList = new ArrayList<>();
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                List<FileMetaInfo> fileMetaInfoList = doGenerateFileList(file, initFid, computeMD5);
                fileList.addAll(fileMetaInfoList);
            } else {
                String name = file.getName();
                long length = file.length();
                long l = file.lastModified();
                String absolutePath = file.getAbsolutePath();
                FileMetaInfo fileMetaInfo = new FileMetaInfo()
                        .setName(name)
                        .setLength(length)
                        .setLastModified(l)
                        .setAbsolutePath(absolutePath);
                if (initFid) {
                    String fid = UUIDUtils.randomUUID();
                    fileMetaInfo.setFid(fid);
                }
                // TODO 计算之后文件被修改，导致MD5不一致问题
                if (computeMD5) {
                    fileMetaInfo.setMd5(SignatureUtils.computeMD5(absolutePath));
                }
                fileList.add(fileMetaInfo);
            }
        }
        return fileList;
    }

    @Override
    public void detectFileListChange() {

        List<FileMetaInfo> filesUnderDirectory = doGenerateFileList(this.directory, false, false);
        filesUnderDirectory.forEach(f -> {
            if (this.latestFileMetaInfoMap.containsKey(f.getAbsolutePath())) {
                FileMetaInfo fileMetaInfo = this.latestFileMetaInfoMap.get(f.getAbsolutePath());
                f.setFid(fileMetaInfo.getFid());
                f.setMd5(fileMetaInfo.getMd5());
            }
        });
        // 新增文件列表
        List<FileMetaInfo> newFileList = filesUnderDirectory.stream()
                .filter(fm -> !this.latestFileMetaInfoMap.containsKey(fm.getAbsolutePath()))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(newFileList)) {
            // 同步新增文件列表信息
            newFileList.forEach(newFile -> {
                newFile.setFid(UUIDUtils.randomUUID());
                newFile.setMd5(SignatureUtils.computeMD5(newFile.getAbsolutePath()));
            });
            log.info("[Generator]detect new file created, file list:{}", fileNames(newFileList));
            sender.syncFileList(ip, port, newFileList);
        }
        // 被修改文件列表
        List<FileMetaInfo> modifiedFileList = filesUnderDirectory.stream()
                .filter(fm -> this.latestFileMetaInfoMap.containsKey(fm.getAbsolutePath()))
                .filter(cur -> fileHasChange(cur, latestFileMetaInfoMap.get(cur.getAbsolutePath())))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(modifiedFileList)) {
            modifiedFileList.forEach(modifiedFile -> {
                String newMD5 = SignatureUtils.computeMD5(modifiedFile.getAbsolutePath());
                if (modifiedFile.getMd5().equalsIgnoreCase(newMD5)) {
                    return;
                }
                modifiedFile.setMd5(newMD5);
                // 查询文件分片信息
                SliceFile sliceFile = sender.fetchFileSliceInfo(ip, port, modifiedFile.getFid());
                if (Objects.isNull(sliceFile)) {
                    log.warn("[Generator]remote file slice info is empty, fid:{}", modifiedFile.getFid());
                    return;
                }
                // 比对文件信息变更、生成文件重构指令
                List<ReconstructFileBlock> reconstructFileBlocks = FileUtils.doSearch(Paths.get(modifiedFile.getAbsolutePath()), sliceFile);
                // 发送文件重构指令
                sender.sendReconstructFileBlocks(ip, port, modifiedFile, reconstructFileBlocks);
            });
        }

        // 更新客户端文件列表信息
        this.latestFileMetaInfoMap = filesUnderDirectory.stream()
                .collect(Collectors.toMap(FileMetaInfo::getAbsolutePath, v -> v));

    }

    private String fileNames(List<FileMetaInfo> newFileList) {
        return newFileList.stream()
                .map(FileMetaInfo::getName)
                .collect(Collectors.joining(","));
    }

    private boolean fileHasChange(FileMetaInfo cur, FileMetaInfo last) {
        return cur.getLength() != last.getLength() || cur.getLastModified() != last.getLastModified();
    }
}
