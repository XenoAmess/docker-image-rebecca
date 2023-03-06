package com.xenoamess.docker.image.rebecca.scan;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.xenoamess.docker.image.rebecca.pojo.FrontSearchResultPojo;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Scanner {

    @NotNull
    public static Collection<String> scan(
            @NotNull String inputFilePath,
            @Nullable Predicate<Map.Entry<@Nullable String, @NotNull FrontSearchResultPojo>> filter
    ) {
        final Collection<String> result = new LinkedHashSet<>();
        Path path = Paths.get( inputFilePath );
        File file = path.toFile();
        if (file.isDirectory()) {
            File[] subFiles = file.listFiles();
            if (subFiles != null) {
                for (File subFile : subFiles) {
                    result.addAll(
                            scan(
                                    subFile.getAbsolutePath(),
                                    filter
                            )
                    );
                }
            }
            return result;
        }
        try (
                InputStream inputStream = Files.newInputStream( path );
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(bufferedInputStream)
        ) {
            boolean success = handleTarFile(
                    inputFilePath,
                    tarArchiveInputStream,
                    null,
                    filter
            );
            if (success) {
                result.add( inputFilePath );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @NotNull
    public static Collection<String> scan(
            @NotNull String inputFilePath
    ) {
        return scan(
                inputFilePath,
                (String) null
        );
    }

    @NotNull
    public static Collection<String> scan(
            @NotNull String inputFilePath,
            @Nullable String fileNameFilterRegexString
    ) {
        return scan(
                inputFilePath,
                new Predicate<Map.Entry<@Nullable String, @NotNull FrontSearchResultPojo>>() {
                    final @Nullable Pattern fileNameFilterRegexPattern = fileNameFilterRegexString != null ? Pattern.compile( fileNameFilterRegexString ) : null;

                    @Override
                    public boolean test(Map.Entry<String, FrontSearchResultPojo> entry) {
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
                }
        );
    }

    private static boolean handleTarFile(
            @NotNull String rootInputTarFileName,
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @Nullable TarArchiveEntry outerInputTarArchiveEntry,
            @Nullable Predicate<Map.Entry<@Nullable String, @NotNull FrontSearchResultPojo>> filter
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
                        if (
                                handleTarFile(
                                        rootInputTarFileName,
                                        tarArchiveInputStream,
                                        inputTarArchiveEntry,
                                        filter
                                )
                        ) {
                            return true;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continue;
                }
                if (handleNormalFile(
                        outerTarArchiveInputStream,
                        inputTarArchiveEntry,
                        filter
                )
                ) {
                    return true;
                }
                ;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (outerInputTarArchiveEntry != null) {
            System.out.println( "tar file handling ended : " + outerInputTarArchiveEntry.getName() );
        }
        return false;
    }

    private static boolean handleNormalFile(
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @NotNull TarArchiveEntry inputTarArchiveEntry,
            @Nullable Predicate<Map.Entry<@Nullable String, @NotNull FrontSearchResultPojo>> filter
    ) {
        final String inputFileName = inputTarArchiveEntry.getName();
        final long inputFileSize = inputTarArchiveEntry.getRealSize();
        System.out.println( "normal file : " + inputFileName );
        if (filter != null) {
            FrontSearchResultPojo scanResultPojo = new FrontSearchResultPojo();
            scanResultPojo.setCount( Integer.MAX_VALUE );
            scanResultPojo.getFileNames().add( inputFileName );
            scanResultPojo.getFileSizes().add( inputFileSize );
            if (filter.test( Pair.of( null, scanResultPojo ) )) {
                System.out.println( "match, continue file : " + inputFileName );
                try {
                    outerTarArchiveInputStream.getNextTarEntry();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            } else {
                try {
                    outerTarArchiveInputStream.getNextTarEntry();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return false;
            }
        }
        try {
            outerTarArchiveInputStream.getNextTarEntry();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    private Scanner() {
    }

}
