package com.xenoamess.docker.image.rebecca.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Base64;

import com.xenoamess.docker.image.rebecca.pojo.ReadAndHashResultPojo;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

public class ReadAndHashUtil {

    private static final byte[] READ_CACHE = new byte[8192];

    @NotNull
    public static ReadAndHashResultPojo readAndHash(
            @NotNull InputStream inputStream
    ) {
        try (
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()
        ) {
            IOUtils.copy(
                    inputStream,
                    byteArrayOutputStream
            );
            MessageDigest messageDigest = MessageDigest.getInstance( "SHA-512" );
            long fileSize = 0;
            byte[] data = byteArrayOutputStream.toByteArray();
            try (
                    InputStream byteArrayInputStream = new ByteArrayInputStream(data);
                    DigestInputStream dis = new DigestInputStream(byteArrayInputStream, messageDigest)
            ) {
                while (true) {
                    int readLength = dis.read( READ_CACHE );
                    if (readLength == -1) {
                        break;
                    }
                    fileSize += readLength;
                }
                /* Read decorated stream (dis) to EOF as normal... */
            }
            byte[] digest = messageDigest.digest();
            String hash = new String(Base64.getUrlEncoder().encode( digest ));
            return new ReadAndHashResultPojo(
                    data,
                    hash
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private ReadAndHashUtil() {
    }

}
