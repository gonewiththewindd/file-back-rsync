package com.gone.file_backup.rsync.utils;

import com.gone.file_backup.file.RollingChecksum;

public class SignatureUtils {

    public static final int M_1 = (1 << 16) - 1;
    public static final int M = 1 << 16;

    public static RollingChecksum checksum(byte[] buffer, int offset, int length, int bufferOffset) {
        int suma = 0, l = offset + length - 1;
        for (int i = offset; i <= l; i++) {
            suma += buffer[i - bufferOffset];
        }
        int a = suma & M_1;

        int sumb = 0;
        for (int i = offset; i <= l; i++) {
            sumb += (l - i + 1) * buffer[i - bufferOffset];
        }
        int b = sumb & M_1;

        int s = a + M * b;

        return new RollingChecksum(a, b, s);
    }

}
