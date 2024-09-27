package com.gone.file_backup.rsync.generator;

import com.gone.file_backup.rsync.model.FileMetaInfo;
import com.gone.file_backup.rsync.utils.UUIDUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class DefaultGeneratorImpl implements Generator {

    private File directory;
    private String destDirectory;
    private Map<String, FileMetaInfo> fileMetaInfoMap = new HashMap<>();

    public DefaultGeneratorImpl(String directory, String destDirectory) {
        this.directory = Paths.get(directory).toFile();
        this.destDirectory = destDirectory;
        if (!this.directory.isDirectory()) {
            throw new IllegalArgumentException(String.format("directory '%s' is not a directory", directory));
        }
    }

    @Override
    public List<FileMetaInfo> generateFileList() {
        log.info("[Generator]Generate file list...");
        List<FileMetaInfo> fileMetaInfos = doGenerateFileList(this.directory, true);
        fileMetaInfos.forEach(f -> {
            // 设置相对路径
            int parentIndex = f.getAbsolutePath().indexOf(this.directory.getAbsolutePath());
            String relativeFilename = f.getAbsolutePath().substring(parentIndex + this.directory.getAbsolutePath().length());
//            String relativePath = relativeFilename.replace(f.getName(), "");
            f.setDestPath(this.destDirectory.concat(relativeFilename));
        });
        this.fileMetaInfoMap = fileMetaInfos.stream()
                .collect(Collectors.toMap(FileMetaInfo::getAbsolutePath, v -> v));
        return fileMetaInfos;
    }

    private List<FileMetaInfo> doGenerateFileList(File dir, boolean initFid) {
        List<FileMetaInfo> fileList = new ArrayList<>();
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                List<FileMetaInfo> fileMetaInfoList = doGenerateFileList(file, initFid);
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
                fileList.add(fileMetaInfo);
            }
        }
        return fileList;
    }

    @Override
    public void detectFileListChange() {

        List<FileMetaInfo> fileMetaInfos = doGenerateFileList(this.directory, false);
        // 新增文件列表
        List<FileMetaInfo> newFileList = fileMetaInfos.stream()
                .filter(fm -> !this.fileMetaInfoMap.containsKey(fm.getAbsolutePath()))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(newFileList)) {
            // 同步新增文件列表信息
        }
        // 被修改文件列表
        List<FileMetaInfo> modifiedFileList = fileMetaInfos.stream()
                .filter(fm -> this.fileMetaInfoMap.containsKey(fm.getAbsolutePath()))
                .filter(cur -> fileHasChange(cur, fileMetaInfoMap.get(cur.getAbsolutePath())))
                .collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(modifiedFileList)) {
            // 查询文件分片信息
            // 比对文件信息变更
            // 生成文件重构指令
            // 发送文件重构指令
        }

        // 更新客户端文件列表信息
        this.fileMetaInfoMap = fileMetaInfos.stream()
                .collect(Collectors.toMap(FileMetaInfo::getAbsolutePath, v -> v));

    }

    private boolean fileHasChange(FileMetaInfo cur, FileMetaInfo last) {
        return cur.getLength() != last.getLength() || cur.getLastModified() != last.getLastModified();
    }
}
