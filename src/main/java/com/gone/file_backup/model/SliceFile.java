package com.gone.file_backup.model;

import com.gone.file_backup.file.FileBlock;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class SliceFile {

    private int blockSize;
    private List<FileBlock> fileBlockList;

}
