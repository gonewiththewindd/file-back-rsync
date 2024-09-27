package com.gone.file_backup.file.reconstruct;

import lombok.Getter;

@Getter
public enum FileBlockTypeEnums {

    MATCHED(1),
    SERIAL(2),
    ;

    private int value;

    FileBlockTypeEnums(int value) {
        this.value = value;
    }
}
