package com.xenoamess.docker.image.rebecca.encode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import com.xenoamess.docker.image.rebecca.pojo.ReadAndHashResultPojo;
import com.xenoamess.docker.image.rebecca.utils.ReadAndHashUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Encoder {

    public static void encode(@NotNull String inputFilePath) {
        encode(inputFilePath, null);
    }

    public static void encode(
            @NotNull String inputFilePath,
            @Nullable String outputFilePath
    ) {
        Map<String, Integer> frontSearchResult = FrontSearcher.frontSearch(inputFilePath);
        Map<String, File> tempDuplicatedFiles = new HashMap<>((frontSearchResult.size() * 4 + 2) / 3);
        try (
                InputStream inputStream = Files.newInputStream(Paths.get(inputFilePath));
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(bufferedInputStream)
        ) {
            String outputFileRebecca;
            if (outputFilePath != null) {
                outputFileRebecca = outputFilePath;
            } else {
                outputFileRebecca = inputFilePath + ".rebecca";
            }
            try (
                    OutputStream outputStream = Files.newOutputStream(Paths.get(outputFileRebecca));
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                    TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(bufferedOutputStream)
            ) {
                Encoder.handleTarFile(
                        inputFilePath,
                        null,
                        false,
                        tarArchiveInputStream,
                        null,
                        tarArchiveOutputStream,
                        tarArchiveOutputStream,
                        frontSearchResult,
                        tempDuplicatedFiles
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (File tempDuplicatedFile : tempDuplicatedFiles.values()) {
            try {
                tempDuplicatedFile.delete();
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    static void handleTarFile(
            @NotNull String rootInputFilePath,
            @Nullable String rootOutputFilePath,
            boolean isRoot,
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @Nullable TarArchiveEntry outerInputTarArchiveEntry,
            @Nullable TarArchiveOutputStream outerTarArchiveOutputStream,
            @NotNull TarArchiveOutputStream rootOuterTarArchiveOutputStream,
            @NotNull Map<String, Integer> frontSearchResult,
            @NotNull Map<String, File> tempDuplicatedFiles
    ) {
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
        try (
                OutputStream outputStream = Files.newOutputStream(Paths.get(outputFileRebecca));
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(bufferedOutputStream)
        ) {
            if (!isRoot) {
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
                        handleTarFile(
                                rootInputFilePath,
                                rootOutputFilePath,
                                false,
                                tarArchiveInputStream,
                                inputTarArchiveEntry,
                                tarArchiveOutputStream,
                                rootOuterTarArchiveOutputStream,
                                frontSearchResult,
                                tempDuplicatedFiles
                        );
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
                handleNormalFile(
                        outerTarArchiveInputStream,
                        inputTarArchiveEntry,
                        tarArchiveOutputStream,
                        rootOuterTarArchiveOutputStream,
                        frontSearchResult,
                        tempDuplicatedFiles
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (outerTarArchiveOutputStream != null) {
            TarArchiveEntry outputTarArchiveEntry = null;
            try {
                if (outerInputTarArchiveEntry != null) {
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
                    outputTarArchiveEntry.setStatusChangeTime(outerInputTarArchiveEntry.getStatusChangeTime());
                    outputTarArchiveEntry.setUserId(outerInputTarArchiveEntry.getLongUserId());
                    outputTarArchiveEntry.setUserName(outerInputTarArchiveEntry.getUserName());
                } else {
                    outputTarArchiveEntry = new TarArchiveEntry(Paths.get(outputFileRebecca));
                    outputTarArchiveEntry.setName("origin.tar");
                    outputTarArchiveEntry.setCreationTime(FileTime.fromMillis(0));
//                    outputTarArchiveEntry.setDevMajor(outerInputTarArchiveEntry.getDevMajor());
//                    outputTarArchiveEntry.setDevMinor(outerInputTarArchiveEntry.getDevMinor());
//                    outputTarArchiveEntry.setGroupId(outerInputTarArchiveEntry.getLongGroupId());
                    outputTarArchiveEntry.setLastAccessTime(FileTime.fromMillis(0));
                    outputTarArchiveEntry.setLastModifiedTime(FileTime.fromMillis(0));
//                    outputTarArchiveEntry.setLinkName(outerInputTarArchiveEntry.getLinkName());
//                    outputTarArchiveEntry.setMode(outerInputTarArchiveEntry.getMode());
                    outputTarArchiveEntry.setModTime(FileTime.fromMillis(0));
//                    outputTarArchiveEntry.setStatusChangeTime(outerInputTarArchiveEntry.getStatusChangeTime());
                    outputTarArchiveEntry.setUserId(0);
//                    outputTarArchiveEntry.setUserName(outerInputTarArchiveEntry.getUserName());
                }


                outerTarArchiveOutputStream.putArchiveEntry(outputTarArchiveEntry);
                IOUtils.copy(Files.newInputStream(Paths.get(outputFileRebecca)), outerTarArchiveOutputStream);
                outputTarArchiveEntry.setSize(outerTarArchiveOutputStream.getBytesWritten());
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
            @NotNull TarArchiveOutputStream rootOuterTarArchiveOutputStream,
            @NotNull Map<String, Integer> frontSearchResult,
            @NotNull Map<String, File> tempDuplicatedFiles
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
            final String hash = readAndHashResultPojo.getHash();
            boolean rebeccaPie;
            if (!frontSearchResult.containsKey(hash)) {
                rebeccaPie = false;
            } else {
                rebeccaPie = handleHashFiles(
                        rootOuterTarArchiveOutputStream,
                        readAndHashResultPojo,
                        tempDuplicatedFiles
                );
            }
            if (rebeccaPie) {
                outputTarArchiveEntry.setName(outputTarArchiveEntry.getName() + ".rebecca_pie");
                byte[] hashStringBytes = readAndHashResultPojo.getHash().getBytes(StandardCharsets.UTF_8);
                outputTarArchiveEntry.setSize(hashStringBytes.length);
                tarArchiveOutputStream.putArchiveEntry(outputTarArchiveEntry);
                tarArchiveOutputStream.write(
                        hashStringBytes
                );
                outputTarArchiveEntry.setSize(tarArchiveOutputStream.getBytesWritten());
                tarArchiveOutputStream.closeArchiveEntry();
            } else {
                tarArchiveOutputStream.putArchiveEntry(outputTarArchiveEntry);
                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(readAndHashResultPojo.getData())) {
                    IOUtils.copy(byteArrayInputStream, tarArchiveOutputStream);
                }
                outputTarArchiveEntry.setSize(tarArchiveOutputStream.getBytesWritten());
                tarArchiveOutputStream.closeArchiveEntry();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean handleHashFiles(
            @NotNull TarArchiveOutputStream rootOuterTarArchiveOutputStream,
            @NotNull ReadAndHashResultPojo readAndHashResultPojo,
            @NotNull Map<String, File> tempDuplicatedFiles
    ) throws IOException {
        File file = tempDuplicatedFiles.get(readAndHashResultPojo.getHash());
        if (file == null) {
            file = File.createTempFile("rebecca-", ".encode");
            file.deleteOnExit();
            FileUtils.writeByteArrayToFile(file, readAndHashResultPojo.getData());
            tempDuplicatedFiles.put(readAndHashResultPojo.getHash(), file);
            TarArchiveEntry fileTarArchiveEntry = new TarArchiveEntry("hash_files/" + readAndHashResultPojo.getHash());
            fileTarArchiveEntry.setSize(readAndHashResultPojo.getData().length);
            fileTarArchiveEntry.setCreationTime(FileTime.fromMillis(0));
//                    outputTarArchiveEntry.setDevMajor(outerInputTarArchiveEntry.getDevMajor());
//                    outputTarArchiveEntry.setDevMinor(outerInputTarArchiveEntry.getDevMinor());
//                    outputTarArchiveEntry.setGroupId(outerInputTarArchiveEntry.getLongGroupId());
            fileTarArchiveEntry.setLastAccessTime(FileTime.fromMillis(0));
            fileTarArchiveEntry.setLastModifiedTime(FileTime.fromMillis(0));
//                    outputTarArchiveEntry.setLinkName(outerInputTarArchiveEntry.getLinkName());
//                    outputTarArchiveEntry.setMode(outerInputTarArchiveEntry.getMode());
            fileTarArchiveEntry.setModTime(FileTime.fromMillis(0));
//                    outputTarArchiveEntry.setStatusChangeTime(outerInputTarArchiveEntry.getStatusChangeTime());
            fileTarArchiveEntry.setUserId(0);
//                    outputTarArchiveEntry.setUserName(outerInputTarArchiveEntry.getUserName());
            rootOuterTarArchiveOutputStream.putArchiveEntry(fileTarArchiveEntry);
            rootOuterTarArchiveOutputStream.write(readAndHashResultPojo.getData());
            fileTarArchiveEntry.setSize(rootOuterTarArchiveOutputStream.getBytesWritten());
            rootOuterTarArchiveOutputStream.closeArchiveEntry();
            return true;
        } else {
            return Arrays.equals(
                    FileUtils.readFileToByteArray(file),
                    readAndHashResultPojo.getData()
            );
        }
    }

}
