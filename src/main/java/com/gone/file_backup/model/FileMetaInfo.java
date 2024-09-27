package com.gone.file_backup.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class FileMetaInfo {

    private String fid; // TODO 不能用随机数ID，必须用md5这种全局一致的充当ID，不然文件多路径备份下，远程会有多个文件
    private String name;
    private long length;
    private long lastModified;
    private String absolutePath;
    private String destPath;
    private String md5;

//    private String syncId;

    SliceFile sliceFile;
}
