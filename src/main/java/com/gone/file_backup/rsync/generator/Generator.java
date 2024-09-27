package com.gone.file_backup.rsync.generator;

import com.gone.file_backup.rsync.model.FileMetaInfo;

import java.util.List;

public interface Generator {
    /**
     * 生成目录下的文件列表
     */
    List<FileMetaInfo> generateFileList();

    void detectFileListChange();

}
