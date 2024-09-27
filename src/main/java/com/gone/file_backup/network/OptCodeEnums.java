package com.gone.file_backup.network;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum OptCodeEnums {

    /**
     * 文件元数据同步
     */
    FILE_META_INFO(101),
    FILE_META_INFO_ACK(-101),

    /**
     * 文件块传输
     */
    FILE_DATA_PART_TRANSPORT(102),
    FILE_DATA_PART_TRANSPORT_ACK(-102),

    /**
     * 文件传输结束
     */
    FILE_DATA_TRANSPORT_FINISH(103),
    FILE_DATA_TRANSPORT_FINISH_ACK(-103),

    /**
     * 全量同步结束(弃用)
     */
    FULL_SYNC_FINISH(104),
    FULL_SYNC_FINISH_ACK(-104),

    /**
     * 文件分片信息
     */
    FILE_SLICE_INFO(105),
    FILE_SLICE_INFO_ACK(-105),

    /**
     * 文件重构块列表传输
     */
    FILE_RECONSTRUCT_LIST_TRANSPORT(106),
    FILE_RECONSTRUCT_LIST_TRANSPORT_ACK(-106),


    /**
     * 文件重构块列表传输结束
     */
    FILE_RECONSTRUCT_LIST_TRANSPORT_FINISH(107),
    FILE_RECONSTRUCT_LIST_TRANSPORT_FINISH_ACK(-107),

    ;

    int value;

    OptCodeEnums(int value) {
        this.value = value;
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
