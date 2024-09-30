package com.gone.file_backup.file.reconstruct;

import com.gone.file_backup.file.RollingChecksum;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@ToString(callSuper = true)
public class MatchedFileBlock extends ReconstructFileBlock {
    String md5;
    String id;
    RollingChecksum blockChecksum;
}
