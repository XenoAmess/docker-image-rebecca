package com.xenoamess.docker.image.rebecca.encode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import com.xenoamess.docker.image.rebecca.pojo.ReadAndHashResultPojo;
import com.xenoamess.docker.image.rebecca.utils.ReadAndHashUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FirstStepEncoder {

    public static void encodeFirstStep(@NotNull String inputFilePath) {
        encodeFirstStep(inputFilePath, null);
    }

    public static void encodeFirstStep(@NotNull String inputFilePath, @Nullable String outputFilePath) {
        Map<String, Integer> frontSearchResult = FrontSearcher.frontSearch(inputFilePath);
        try (InputStream inputStream = Files.newInputStream(Paths.get(inputFilePath)); BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream); TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(bufferedInputStream);) {
            FirstStepEncoder.handleTarFile(inputFilePath, outputFilePath, true, tarArchiveInputStream, null, null, frontSearchResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Deprecated
    @SuppressWarnings("RedundantArrayCreation")
    private static final List<Pattern> FILTER_FILE_PATTERN = Arrays.asList(new Pattern[]{
            //backend
            Pattern.compile("^.*\\.jar$"), Pattern.compile("^.*\\.py$"),
            //frontend
            Pattern.compile("^.*\\.html$"), Pattern.compile("^.*\\.css$"), Pattern.compile("^.*\\.js$"), Pattern.compile("^.*\\.ts$"),
            //config
            Pattern.compile("^.*\\.conf$"), Pattern.compile("^.*\\.ini$"),
            //zip
            Pattern.compile("^.*\\.zip$"), Pattern.compile("^.*\\.gz$"), Pattern.compile("^.*\\.bz$"), Pattern.compile("^.*\\.bz2$"),
            //text
            Pattern.compile("^.*\\.md$"), Pattern.compile("^.*\\.txt$"),
            //data
            Pattern.compile("^.*\\.json$"), Pattern.compile("^.*\\.dtd$"), Pattern.compile("^.*\\.xml$"), Pattern.compile("^.*\\.properties$"), Pattern.compile("^.*\\.dat$"), Pattern.compile("^.*\\.data$"),
            //image
            Pattern.compile("^.*\\.ttf$"), Pattern.compile("^.*\\.jpg$"), Pattern.compile("^.*\\.bpm$"),
            //doc
            Pattern.compile("^.*\\.doc$"), Pattern.compile("^.*\\.pdf$"),
            //license
            Pattern.compile("^VERSION$"), Pattern.compile("^LICENSE$"), Pattern.compile("^ASSEMBLY_EXCEPTION$"), Pattern.compile("^ADDITIONAL_LICENSE_INFO$"),});

    static void handleTarFile(@NotNull String rootInputFilePath, @Nullable String rootOutputFilePath, boolean isRoot, @NotNull TarArchiveInputStream outerTarArchiveInputStream, @Nullable TarArchiveEntry outerInputTarArchiveEntry, @Nullable TarArchiveOutputStream outerTarArchiveOutputStream, @NotNull Map<String, Integer> frontSearchResult) {
        if (outerInputTarArchiveEntry != null) {
            System.out.println("tar file handling started : " + outerInputTarArchiveEntry.getName());
        }

        String outputFileRebecca;
        if (isRoot && rootOutputFilePath != null) {
            outputFileRebecca = rootOutputFilePath;
        } else {
            String outputFileOri;
            if (outerInputTarArchiveEntry == null) {
                outputFileOri = rootInputFilePath + ".ori";
            } else {
                outputFileOri = rootInputFilePath + "." + UUID.randomUUID() + ".ori";
            }
            outputFileRebecca = outputFileOri + ".rebecca";
        }

        File outputFile = Paths.get(outputFileRebecca).toFile();
        try {
            outputFile.getParentFile().mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try (OutputStream outputStream = Files.newOutputStream(Paths.get(outputFileRebecca)); BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream); TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(bufferedOutputStream);) {
            if (outerInputTarArchiveEntry != null) {
                Paths.get(outputFileRebecca).toFile().deleteOnExit();
            }
            while (true) {
                TarArchiveEntry inputTarArchiveEntry = outerTarArchiveInputStream.getNextTarEntry();
                if (inputTarArchiveEntry == null) {
                    break;
                }
                if (inputTarArchiveEntry.getName().endsWith(".tar")) {
                    String outputFileOri2 = rootInputFilePath + "." + UUID.randomUUID() + ".ori";
                    String outputFileRebecca2 = outputFileOri2 + ".rebecca";
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(outerTarArchiveInputStream);
                    TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(bufferedInputStream);
                    try {
                        handleTarFile(rootInputFilePath, rootOutputFilePath, false, tarArchiveInputStream, inputTarArchiveEntry, tarArchiveOutputStream, frontSearchResult);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    continue;
                }
//                boolean fileMatched = false;
//                for (Pattern pattern : FILTER_FILE_PATTERN) {
//                    if (pattern.matcher(inputTarArchiveEntry.getName()).matches()) {
//                        fileMatched = true;
//                        break;
//                    }
//                }
//                if (fileMatched) {
//                    handleMatchedFile(
//                            outerTarArchiveInputStream,
//                            inputTarArchiveEntry,
//                            tarArchiveOutputStream
//                    );
//                    continue;
//                }
                handleNormalFile(outerTarArchiveInputStream, inputTarArchiveEntry, tarArchiveOutputStream, frontSearchResult);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (outerTarArchiveOutputStream != null) {
            TarArchiveEntry outputTarArchiveEntry = null;
            try {
                outputTarArchiveEntry = new TarArchiveEntry(Paths.get(outputFileRebecca));
                outputTarArchiveEntry.setName(outerInputTarArchiveEntry.getName());
                outputTarArchiveEntry.setCreationTime(outerInputTarArchiveEntry.getCreationTime());
                outputTarArchiveEntry.setDevMajor(outerInputTarArchiveEntry.getDevMajor());
                outputTarArchiveEntry.setDevMinor(outerInputTarArchiveEntry.getDevMinor());
                outputTarArchiveEntry.setGroupId(outerInputTarArchiveEntry.getLongGroupId());
                outputTarArchiveEntry.setLastAccessTime(outerInputTarArchiveEntry.getLastAccessTime());
                outputTarArchiveEntry.setLastModifiedTime(outerInputTarArchiveEntry.getLastModifiedTime());
                outputTarArchiveEntry.setLinkName(outerInputTarArchiveEntry.getLinkName());
                outputTarArchiveEntry.setMode(outerInputTarArchiveEntry.getMode());
                outputTarArchiveEntry.setModTime(outerInputTarArchiveEntry.getModTime());
                outputTarArchiveEntry.setSize(outerInputTarArchiveEntry.getSize());
                outputTarArchiveEntry.setStatusChangeTime(outerInputTarArchiveEntry.getStatusChangeTime());
                outputTarArchiveEntry.setUserId(outerInputTarArchiveEntry.getLongUserId());
                outputTarArchiveEntry.setUserName(outerInputTarArchiveEntry.getUserName());
                outerTarArchiveOutputStream.putArchiveEntry(outputTarArchiveEntry);
                IOUtils.copy(Files.newInputStream(Paths.get(outputFileRebecca)), outerTarArchiveOutputStream);
                outerTarArchiveOutputStream.closeArchiveEntry();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (outerInputTarArchiveEntry != null) {
            System.out.println("tar file handling ended : " + outerInputTarArchiveEntry.getName());
        }
    }

//    private static void handleMatchedFile(
//            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
//            @NotNull TarArchiveEntry inputTarArchiveEntry,
//            @NotNull TarArchiveOutputStream tarArchiveOutputStream
//    ) {
//        System.out.println("matched file : " + inputTarArchiveEntry.getName());
//        handleNormalFile(
//                outerTarArchiveInputStream,
//                inputTarArchiveEntry,
//                tarArchiveOutputStream
//        );
//    }

    private static void handleNormalFile(
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @NotNull TarArchiveEntry inputTarArchiveEntry,
            @NotNull TarArchiveOutputStream tarArchiveOutputStream,
            @NotNull Map<String, Integer> frontSearchResult
    ) {
        System.out.println("normal file : " + inputTarArchiveEntry.getName());
        TarArchiveEntry outputTarArchiveEntry = new TarArchiveEntry(inputTarArchiveEntry.getName());
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
            ReadAndHashResultPojo readAndHashResultPojo = ReadAndHashUtil.readAndHash(
                    outerTarArchiveInputStream
            );
            if (readAndHashResultPojo.getData().length < 1024) {
                // too small file have no compress value
                return;
            }
            final String hash = readAndHashResultPojo.getHash();
            if (!frontSearchResult.containsKey(hash)) {
                tarArchiveOutputStream.putArchiveEntry(outputTarArchiveEntry);
                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(readAndHashResultPojo.getData())) {
                    IOUtils.copy(byteArrayInputStream, tarArchiveOutputStream);
                }
                tarArchiveOutputStream.closeArchiveEntry();
            } else {
                outputTarArchiveEntry.setName(outputTarArchiveEntry.getName() + ".rebecca_pie");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
