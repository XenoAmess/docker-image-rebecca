package com.xenoamess.docker.image.rebecca.encode;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.regex.Pattern;

import com.xenoamess.docker.image.rebecca.pojo.FrontSearchResultPojo;
import com.xenoamess.docker.image.rebecca.pojo.ReadAndHashResultPojo;
import com.xenoamess.docker.image.rebecca.utils.ReadAndHashUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FrontSearcher {

    @NotNull
    public static Map<String, FrontSearchResultPojo> frontSearch(
            @NotNull String inputFilePath,
            @Nullable Function<Map.Entry<String, FrontSearchResultPojo>, Boolean> filter
    ) {
        final Map<String, FrontSearchResultPojo> hashToCountPre = new HashMap<>();
        try (
                InputStream inputStream = Files.newInputStream( Paths.get( inputFilePath ) );
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(bufferedInputStream)
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
        HashMap<String, FrontSearchResultPojo> hashMap = new HashMap<>(hashToCountPre.size());
        for (Map.Entry<String, FrontSearchResultPojo> entry : hashToCountPre.entrySet()) {
            if (filter != null) {
                if (
                        !Boolean.TRUE.equals(
                                filter.apply(
                                        Pair.of(
                                                entry.getKey(),
                                                entry.getValue()
                                        )
                                )
                        )
                ) {
                    continue;
                }
            }
            hashMap.put(
                    entry.getKey(),
                    entry.getValue()
            );
        }
        return hashMap;
    }

    @NotNull
    public static Map<String, FrontSearchResultPojo> frontSearch(
            @NotNull String inputFilePath
    ) {
        return frontSearch(
                inputFilePath,
                (String) null
        );
    }

    @NotNull
    public static Map<String, FrontSearchResultPojo> frontSearch(
            @NotNull String inputFilePath,
            @Nullable String fileNameFilterRegexString
    ) {
        final Pattern fileNameFilterRegexPattern = fileNameFilterRegexString != null ? Pattern.compile( fileNameFilterRegexString ) : null;
        return frontSearch(
                inputFilePath,
                entry -> {
                    if (entry.getValue().getCount() <= 1) {
                        // no need to rebecca if only 1
                        return false;
                    }
                    for (long fileSize : entry.getValue().getFileSizes()) {
                        if (fileSize < 1024 * 4) {
                            // too small file have no compress value
                            return false;
                        }
                    }
                    if (fileNameFilterRegexPattern != null) {
                        for (String fileName : entry.getValue().getFileNames()) {
                            if (!fileNameFilterRegexPattern.matcher( fileName ).matches()) {
                                // any file name not match fileNameFilterRegexPattern, if here be
                                return false;
                            }
                        }
                    }
                    return true;
                }
        );
    }

    private static void handleTarFile(
            @NotNull String rootInputTarFileName,
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @Nullable TarArchiveEntry outerInputTarArchiveEntry,
            @NotNull Map<String, FrontSearchResultPojo> hashToCountPre
    ) {
        if (outerInputTarArchiveEntry != null) {
            System.out.println( "tar file handling started : " + outerInputTarArchiveEntry.getName() );
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
                Paths.get( outputFileRebecca ).toFile().deleteOnExit();
            }
            while (true) {
                TarArchiveEntry inputTarArchiveEntry = outerTarArchiveInputStream.getNextTarEntry();
                if (inputTarArchiveEntry == null) {
                    break;
                }
                if (inputTarArchiveEntry.getName().endsWith( ".tar" )) {
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
            System.out.println( "tar file handling ended : " + outerInputTarArchiveEntry.getName() );
        }
    }

    private static void handleNormalFile(
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @NotNull TarArchiveEntry inputTarArchiveEntry,
            @NotNull Map<String, FrontSearchResultPojo> hashToCountPre
    ) {
        System.out.println( "normal file : " + inputTarArchiveEntry.getName() );
        TarArchiveEntry outputTarArchiveEntry = new TarArchiveEntry(
                inputTarArchiveEntry.getName()
        );
        try {
            outputTarArchiveEntry.setName( inputTarArchiveEntry.getName() );
            outputTarArchiveEntry.setCreationTime( inputTarArchiveEntry.getCreationTime() );
            outputTarArchiveEntry.setDevMajor( inputTarArchiveEntry.getDevMajor() );
            outputTarArchiveEntry.setDevMinor( inputTarArchiveEntry.getDevMinor() );
            outputTarArchiveEntry.setGroupId( inputTarArchiveEntry.getLongGroupId() );
            outputTarArchiveEntry.setLastAccessTime( inputTarArchiveEntry.getLastAccessTime() );
            outputTarArchiveEntry.setLastModifiedTime( inputTarArchiveEntry.getLastModifiedTime() );
            outputTarArchiveEntry.setLinkName( inputTarArchiveEntry.getLinkName() );
            outputTarArchiveEntry.setMode( inputTarArchiveEntry.getMode() );
            outputTarArchiveEntry.setModTime( inputTarArchiveEntry.getModTime() );
            outputTarArchiveEntry.setSize( inputTarArchiveEntry.getSize() );
            outputTarArchiveEntry.setStatusChangeTime( inputTarArchiveEntry.getStatusChangeTime() );
            outputTarArchiveEntry.setUserId( inputTarArchiveEntry.getLongUserId() );
            outputTarArchiveEntry.setUserName( inputTarArchiveEntry.getUserName() );

            ReadAndHashResultPojo readAndHashResultPojo = ReadAndHashUtil.readAndHash(
                    outerTarArchiveInputStream
            );
            final String hash = readAndHashResultPojo.getHash();
            FrontSearchResultPojo frontSearchResultPojo = hashToCountPre.get( hash );
            if (frontSearchResultPojo == null) {
                frontSearchResultPojo = new FrontSearchResultPojo();
                frontSearchResultPojo.setCount( 1 );
                hashToCountPre.put( hash, frontSearchResultPojo );
            } else {
                frontSearchResultPojo.setCount( frontSearchResultPojo.getCount() + 1 );
            }
            frontSearchResultPojo.getFileNames().add( inputTarArchiveEntry.getName() );
            frontSearchResultPojo.getFileSizes().add( (long) readAndHashResultPojo.getData().length );
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private FrontSearcher() {
    }

}
