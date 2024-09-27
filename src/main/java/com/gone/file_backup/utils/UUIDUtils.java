package com.gone.file_backup.utils;

import java.util.UUID;

public class UUIDUtils {


    public static final String randomUUID() {
        return UUID.randomUUID().toString().replace("-", "");
    }

}
