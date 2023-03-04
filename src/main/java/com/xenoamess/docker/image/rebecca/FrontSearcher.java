package com.xenoamess.docker.image.rebecca;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class FrontSearcher {

    @NotNull
    static Map<String, Integer> frontSearch(
            @NotNull String inputFilePath,
            @Nullable Function<Map.Entry<String, Integer>, Boolean> filter
    ) {
        final Map<String, AtomicInteger> hashToCountPre = new ConcurrentHashMap<>();
        try (
                InputStream inputStream = Files.newInputStream(Paths.get(inputFilePath));
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(bufferedInputStream);
        ) {
            handleTarFile(
                    inputFilePath,
                    tarArchiveInputStream,
                    null,
                    hashToCountPre
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        HashMap<String, Integer> hashMap = new HashMap<>(hashToCountPre.size());
        for (Map.Entry<String, AtomicInteger> entry : hashToCountPre.entrySet()) {
            if (filter != null) {
                if (
                        !Boolean.TRUE.equals(
                                filter.apply(
                                        Pair.of(
                                                entry.getKey(),
                                                entry.getValue().get()
                                        )
                                )
                        )
                ) {
                    continue;
                }
            }
            hashMap.put(
                    entry.getKey(),
                    entry.getValue().get()
            );
        }
        return hashMap;
    }

    @NotNull
    public static Map<String, Integer> frontSearch(
            @NotNull String inputFilePath
    ) {
        return frontSearch(
                inputFilePath,
                entry -> {
                    // no need to rebecca if only 1
                    return entry.getValue() > 1;
                }
        );
    }

    private static void handleTarFile(
            @NotNull String rootInputTarFileName,
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @Nullable TarArchiveEntry outerInputTarArchiveEntry,
            @NotNull Map<String, AtomicInteger> hashToCountPre
    ) {
        if (outerInputTarArchiveEntry != null) {
            System.out.println("tar file handling started : " + outerInputTarArchiveEntry.getName());
        }
        String outputFileOri;
        if (outerInputTarArchiveEntry == null) {
            outputFileOri = rootInputTarFileName + ".ori";
        } else {
            outputFileOri = rootInputTarFileName + "." + UUID.randomUUID() + ".ori";
        }
        String outputFileRebecca = outputFileOri + ".rebecca";
        try {
            if (outerInputTarArchiveEntry != null) {
                Paths.get(outputFileRebecca).toFile().deleteOnExit();
            }
            while (true) {
                TarArchiveEntry inputTarArchiveEntry = outerTarArchiveInputStream.getNextTarEntry();
                if (inputTarArchiveEntry == null) {
                    break;
                }
                if (inputTarArchiveEntry.getName().endsWith(".tar")) {
                    String outputFileOri2 = rootInputTarFileName + "." + UUID.randomUUID() + ".ori";
                    String outputFileRebecca2 = outputFileOri2 + ".rebecca";
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(outerTarArchiveInputStream);
                    TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(bufferedInputStream);
                    try {
                        handleTarFile(
                                rootInputTarFileName,
                                tarArchiveInputStream,
                                inputTarArchiveEntry,
                                hashToCountPre
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                handleNormalFile(
                        outerTarArchiveInputStream,
                        inputTarArchiveEntry,
                        hashToCountPre
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (outerInputTarArchiveEntry != null) {
            System.out.println("tar file handling ended : " + outerInputTarArchiveEntry.getName());
        }
    }

    private static final byte[] READ_CACHE = new byte[8192];

    private static void handleNormalFile(
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @NotNull TarArchiveEntry inputTarArchiveEntry,
            @NotNull Map<String, AtomicInteger> hashToCountPre
    ) {
        System.out.println("normal file : " + inputTarArchiveEntry.getName());
        TarArchiveEntry outputTarArchiveEntry = new TarArchiveEntry(
                inputTarArchiveEntry.getName()
        );
        try {
            outputTarArchiveEntry.setName(inputTarArchiveEntry.getName());
            outputTarArchiveEntry.setCreationTime(inputTarArchiveEntry.getCreationTime());
            outputTarArchiveEntry.setDevMajor(inputTarArchiveEntry.getDevMajor());
            outputTarArchiveEntry.setDevMinor(inputTarArchiveEntry.getDevMinor());
            outputTarArchiveEntry.setGroupId(inputTarArchiveEntry.getLongGroupId());
            outputTarArchiveEntry.setLastAccessTime(inputTarArchiveEntry.getLastAccessTime());
            outputTarArchiveEntry.setLastModifiedTime(inputTarArchiveEntry.getLastModifiedTime());
            outputTarArchiveEntry.setLinkName(inputTarArchiveEntry.getLinkName());
            outputTarArchiveEntry.setMode(inputTarArchiveEntry.getMode());
            outputTarArchiveEntry.setModTime(inputTarArchiveEntry.getModTime());
            outputTarArchiveEntry.setSize(inputTarArchiveEntry.getSize());
            outputTarArchiveEntry.setStatusChangeTime(inputTarArchiveEntry.getStatusChangeTime());
            outputTarArchiveEntry.setUserId(inputTarArchiveEntry.getLongUserId());
            outputTarArchiveEntry.setUserName(inputTarArchiveEntry.getUserName());

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            IOUtils.copy(
                    outerTarArchiveInputStream,
                    byteArrayOutputStream
            );
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            long fileSize = 0;
            try (
                    InputStream inputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
                    DigestInputStream dis = new DigestInputStream(inputStream, messageDigest)
            ) {
                while (true) {
                    int readLength = dis.read(READ_CACHE);
                    if (readLength == -1) {
                        break;
                    }
                    fileSize += readLength;
                }
                /* Read decorated stream (dis) to EOF as normal... */
            }
            if (fileSize < 1024) {
                // too small file have no compress value
                return;
            }
            byte[] digest = messageDigest.digest();
            String hash = new String(Base64.getUrlEncoder().encode(digest));
            String key = hash + "|" + inputTarArchiveEntry.getName();
            AtomicInteger count = hashToCountPre.get(key);
            if (count == null) {
                hashToCountPre.put(key, new AtomicInteger(1));
            } else {
                count.incrementAndGet();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
