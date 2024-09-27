package com.gone.file_backup.model;

import com.gone.file_backup.file.reconstruct.ReconstructFileBlock;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Data
@Accessors(chain = true)
public class ReconstructFile {

    private String fid;
    private String md5;
    private Set<ReconstructFileBlock> blocks;

    public ReconstructFile() {
        this.blocks = new CopyOnWriteArraySet<>();
    }

}
