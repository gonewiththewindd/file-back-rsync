package com.gone.file_backup.rsync.utils;

import java.util.zip.CRC32;

public class Crc32Utils {

    public static long computeCrc32(byte[] bytes){
        CRC32 crc32 = new CRC32();
        crc32.update(bytes);
        return crc32.getValue();
    }


}
