package com.gone.file_backup.utils;

import com.gone.file_backup.file.RollingChecksum;
import org.springframework.util.DigestUtils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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

    public static RollingChecksum nextBlockCheckSum(byte[] bytes, int l, int k, int bufferOffset, RollingChecksum checksum) {

        int an = (checksum.getA() - bytes[k - bufferOffset] + bytes[l - bufferOffset]) & M_1;
        int bn = (checksum.getB() - (l - k) * bytes[k - bufferOffset] + an) & M_1;
        int sn = an + M * bn;

        return new RollingChecksum(an, bn, sn);
    }


    public static final String computeMD5(String path) {
        try (BufferedInputStream bufferedInputStream = new BufferedInputStream(Files.newInputStream(Paths.get(path)))) {
            return DigestUtils.md5DigestAsHex(bufferedInputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
