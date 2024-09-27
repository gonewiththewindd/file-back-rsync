package com.gone.file_backup.rsync.network;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum OptCodeEnums {

    SEND_FILE_META_INFO(101),
    SEND_FILE_META_INFO_ACK(-101),

    SEND_FILE_DATA(102),
    SEND_FILE_DATA_ACK(-102),

    FULL_SYNC_FINISH(103),
    FULL_SYNC_FINISH_ACK(-103),

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
