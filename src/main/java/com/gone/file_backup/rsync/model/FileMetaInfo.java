package com.gone.file_backup.rsync.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class FileMetaInfo {
    private String fid;
    private String name;
    private long length;
    private long lastModified;
    private String absolutePath;
    private String destPath;
}
