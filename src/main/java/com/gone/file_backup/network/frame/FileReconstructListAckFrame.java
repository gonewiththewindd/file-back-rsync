package com.gone.file_backup.network.frame;

import com.gone.file_backup.network.OptCodeEnums;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileReconstructListAckFrame extends OperationBaseFrame {

    private FileReconstructListAckFrame() {
        super.optCode = OptCodeEnums.FILE_RECONSTRUCT_LIST_TRANSPORT_ACK;
    }
    
}
