package com.gone.file_backup.network.frame;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileReconstructBlocksAckFrame extends OperationBaseFrame {

    public FileReconstructBlocksAckFrame() {
    }
    
}
