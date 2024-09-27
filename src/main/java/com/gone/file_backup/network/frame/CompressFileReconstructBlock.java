package com.gone.file_backup.network.frame;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(chain = true)
public class CompressFileReconstructBlock {

    private String fid;
    private String md5;
    private List<CompactMatchFileBlock> blocks;

    @Data
    @Accessors(chain = true)
    public static class CompactMatchFileBlock {
        private String id;
        private int from;
        private int to;
    }

}
