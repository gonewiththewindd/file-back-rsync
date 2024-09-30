package com.gone.file_backup.network;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum OptCodeEnums {

    /**
     * 文件元数据同步
     */
    FILE_META_INFO(101, "metaInfoOpt"),
    FILE_META_INFO_ACK(-101, "metaInfoAckOpt"),

    /**
     * 文件块传输
     */
    FILE_DATA_PART(102, "fileDataPartOpt"),
    FILE_DATA_PART_ACK(-102, "fileDataPartAckOpt"),

    /**
     * 文件传输结束
     */
    FILE_DATA_FINISH(103, "fileDataFinishOpt"),
    FILE_DATA_FINISH_ACK(-103, "fileDataFinishAckOpt"),

    /**
     * 全量同步结束(弃用)
     */
    FULL_SYNC_FINISH(104, "MetaInfoOpt"),
    FULL_SYNC_FINISH_ACK(-104, "MetaInfoOpt"),

    /**
     * 文件分片信息
     */
    FILE_SLICE_INFO(105, "fileSliceInfoOpt"),
    FILE_SLICE_INFO_ACK(-105, "fileSliceInfoAckOpt"),

    /**
     * 文件重构块列表
     */
    FILE_RECONSTRUCT_BLOCKS(106, "fileReconstructBlocksOpt"),
    FILE_RECONSTRUCT_BLOCKS_ACK(-106, "fileReconstructBlocksAckOpt"),

    /**
     * 文件重构
     */
    FILE_RECONSTRUCT(107, "fileReconstructOpt"),
    FILE_RECONSTRUCT_ACK(-107, "fileReconstructAckOpt"),

    ;

    int value;
    String beanName;

    OptCodeEnums(int value, String beanName) {
        this.value = value;
        this.beanName = beanName;
    }

    public static OptCodeEnums of(int optCode) {
        return Arrays.stream(values())
                .filter(e -> e.getValue() == optCode)
                .findFirst()
                .orElseThrow(() -> {
                    throw new IllegalArgumentException(String.format("Unknown opt code %d", optCode));
                });
    }
}
