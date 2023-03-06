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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.xenoamess.docker.image.rebecca.pojo.FrontSearchResultPojo;
import com.xenoamess.docker.image.rebecca.pojo.ReadAndHashResultPojo;
import com.xenoamess.docker.image.rebecca.utils.ReadAndHashUtil;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarConstants;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.xenoamess.docker.image.rebecca.encode.FrontSearcher.isLinkFile;

public class Encoder {

    public static void encode(@NotNull String inputFilePath) {
        encode(
                inputFilePath,
                null
        );
    }

    public static void encode(
            @NotNull String inputFilePath,
            @Nullable String outputFilePath
    ) {
        encode(
                inputFilePath,
                outputFilePath,
                null
        );
    }

    public static void encode(
            @NotNull String inputFilePath,
            @Nullable String outputFilePath,
            @Nullable String fileNameFilterRegexString
    ) {
        Map<String, FrontSearchResultPojo> frontSearchResult = FrontSearcher.frontSearch(
                inputFilePath,
                fileNameFilterRegexString
        );
        Map<String, File> tempDuplicatedFiles = new HashMap<>((frontSearchResult.size() * 4 + 2) / 3);
        try (
                InputStream inputStream = Files.newInputStream( Paths.get( inputFilePath ) );
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
        ) {
            String outputFileRebecca;
            if (outputFilePath != null) {
                outputFileRebecca = outputFilePath;
            } else {
                outputFileRebecca = inputFilePath + ".rebecca";
            }
            Path outPath = Paths.get( outputFileRebecca );
            try {
                File outputFile = outPath.toFile();
                File parentFile = outputFile.getParentFile();
                if (parentFile != null) {
                    parentFile = parentFile.getAbsoluteFile();
                    if (parentFile != null) {
                        parentFile.mkdirs();
                    }
                }
            } catch (Exception ignored) {
            }
            try (
                    OutputStream outputStream = Files.newOutputStream( outPath );
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                    TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(bufferedOutputStream)
            ) {
                tarArchiveOutputStream.setBigNumberMode( TarArchiveOutputStream.BIGNUMBER_POSIX );
                tarArchiveOutputStream.setLongFileMode( TarArchiveOutputStream.LONGFILE_POSIX );
                Encoder.handleTarFile(
                        inputFilePath,
                        null,
                        false,
                        bufferedInputStream,
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

    static boolean handleTarFile(
            @NotNull String rootInputFilePath,
            @Nullable String rootOutputFilePath,
            boolean isRoot,
            @NotNull BufferedInputStream outerBufferedInputStream,
            @Nullable TarArchiveEntry outerInputTarArchiveEntry,
            @Nullable TarArchiveOutputStream outerTarArchiveOutputStream,
            @NotNull TarArchiveOutputStream rootOuterTarArchiveOutputStream,
            @NotNull Map<String, FrontSearchResultPojo> frontSearchResult,
            @NotNull Map<String, File> tempDuplicatedFiles
    ) {
        String tmpInputFilePath = rootInputFilePath + "." + UUID.randomUUID() + ".tmp";
        Paths.get( tmpInputFilePath ).toFile().deleteOnExit();
        boolean changed = false;
        try (
                OutputStream outputStream = Files.newOutputStream( Paths.get( tmpInputFilePath ) );
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream)
        ) {
            IOUtils.copy(
                    outerBufferedInputStream,
                    bufferedOutputStream
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        try (
                InputStream inputStream = Files.newInputStream( Paths.get( tmpInputFilePath ) );
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                TarArchiveInputStream outerTarArchiveInputStream = new TarArchiveInputStream(bufferedInputStream)
        ) {
            if (outerInputTarArchiveEntry != null) {
                System.out.println( "tar file handling started : " + outerInputTarArchiveEntry.getName() );
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

            try {
                File outputFile = Paths.get( outputFileRebecca ).toFile();
                File parentFile = outputFile.getParentFile();
                if (parentFile != null) {
                    parentFile = parentFile.getAbsoluteFile();
                    if (parentFile != null) {
                        parentFile.mkdirs();
                    }
                }
            } catch (Exception ignored) {
            }
            try (
                    OutputStream outputStream = Files.newOutputStream( Paths.get( outputFileRebecca ) );
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
                    TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(bufferedOutputStream)
            ) {
                tarArchiveOutputStream.setBigNumberMode( TarArchiveOutputStream.BIGNUMBER_POSIX );
                tarArchiveOutputStream.setLongFileMode( TarArchiveOutputStream.LONGFILE_POSIX );
                if (!isRoot) {
                    Paths.get( outputFileRebecca ).toFile().deleteOnExit();
                }
                while (true) {
                    TarArchiveEntry inputTarArchiveEntry = outerTarArchiveInputStream.getNextTarEntry();
                    if (inputTarArchiveEntry == null) {
                        break;
                    }
                    if (inputTarArchiveEntry.isDirectory()) {
                        handleDirectory(
                                outerTarArchiveInputStream,
                                inputTarArchiveEntry,
                                tarArchiveOutputStream
                        );
                        continue;
                    }
                    if (isLinkFile( inputTarArchiveEntry )) {
                        handleLinkFile(
                                outerTarArchiveInputStream,
                                inputTarArchiveEntry,
                                tarArchiveOutputStream
                        );
                        continue;
                    }
                    if (inputTarArchiveEntry.getName().endsWith( ".tar" )) {
                        String outputFileOri2 = rootInputFilePath + "." + UUID.randomUUID() + ".ori";
                        String outputFileRebecca2 = outputFileOri2 + ".rebecca";
                        BufferedInputStream bufferedInputStream2 = new BufferedInputStream(outerTarArchiveInputStream);
                        try {
                            boolean result = handleTarFile(
                                    rootInputFilePath,
                                    rootOutputFilePath,
                                    false,
                                    bufferedInputStream2,
                                    inputTarArchiveEntry,
                                    tarArchiveOutputStream,
                                    rootOuterTarArchiveOutputStream,
                                    frontSearchResult,
                                    tempDuplicatedFiles
                            );
                            if (result) {
                                changed = true;
                            }
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
                    boolean result = handleNormalFile(
                            outerTarArchiveInputStream,
                            inputTarArchiveEntry,
                            tarArchiveOutputStream,
                            rootOuterTarArchiveOutputStream,
                            frontSearchResult,
                            tempDuplicatedFiles
                    );
                    if (result) {
                        changed = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (outerTarArchiveOutputStream != null) {
                TarArchiveEntry outputTarArchiveEntry = null;
                try {
                    if (outerInputTarArchiveEntry != null) {
                        outputTarArchiveEntry = new TarArchiveEntry(Paths.get( outputFileRebecca ));
                        outputTarArchiveEntry.setName( outerInputTarArchiveEntry.getName() );
                        outputTarArchiveEntry.setCreationTime( outerInputTarArchiveEntry.getCreationTime() );
                        outputTarArchiveEntry.setDevMajor( outerInputTarArchiveEntry.getDevMajor() );
                        outputTarArchiveEntry.setDevMinor( outerInputTarArchiveEntry.getDevMinor() );
                        outputTarArchiveEntry.setGroupId( outerInputTarArchiveEntry.getLongGroupId() );
                        outputTarArchiveEntry.setLastAccessTime( outerInputTarArchiveEntry.getLastAccessTime() );
                        outputTarArchiveEntry.setLastModifiedTime( outerInputTarArchiveEntry.getLastModifiedTime() );
                        outputTarArchiveEntry.setLinkName( outerInputTarArchiveEntry.getLinkName() );
                        outputTarArchiveEntry.setMode( outerInputTarArchiveEntry.getMode() );
                        outputTarArchiveEntry.setModTime( outerInputTarArchiveEntry.getModTime() );
                        outputTarArchiveEntry.setStatusChangeTime( outerInputTarArchiveEntry.getStatusChangeTime() );
                        outputTarArchiveEntry.setUserId( outerInputTarArchiveEntry.getLongUserId() );
                        outputTarArchiveEntry.setUserName( outerInputTarArchiveEntry.getUserName() );
                    } else {
                        outputTarArchiveEntry = new TarArchiveEntry(Paths.get( outputFileRebecca ));
                        outputTarArchiveEntry.setName( "origin.tar" );
                        outputTarArchiveEntry.setCreationTime( FileTime.fromMillis( 0 ) );
//                    outputTarArchiveEntry.setDevMajor(outerInputTarArchiveEntry.getDevMajor());
//                    outputTarArchiveEntry.setDevMinor(outerInputTarArchiveEntry.getDevMinor());
//                    outputTarArchiveEntry.setGroupId(outerInputTarArchiveEntry.getLongGroupId());
                        outputTarArchiveEntry.setLastAccessTime( FileTime.fromMillis( 0 ) );
                        outputTarArchiveEntry.setLastModifiedTime( FileTime.fromMillis( 0 ) );
//                    outputTarArchiveEntry.setLinkName(outerInputTarArchiveEntry.getLinkName());
//                    outputTarArchiveEntry.setMode(outerInputTarArchiveEntry.getMode());
                        outputTarArchiveEntry.setModTime( FileTime.fromMillis( 0 ) );
//                    outputTarArchiveEntry.setStatusChangeTime(outerInputTarArchiveEntry.getStatusChangeTime());
                        outputTarArchiveEntry.setUserId( 0 );
//                    outputTarArchiveEntry.setUserName(outerInputTarArchiveEntry.getUserName());
                    }
                    if (changed) {
                        outputTarArchiveEntry.setSize(
                                Files.size( Paths.get( outputFileRebecca ) )
                        );
                        outerTarArchiveOutputStream.putArchiveEntry( outputTarArchiveEntry );
                        IOUtils.copy( Files.newInputStream( Paths.get( outputFileRebecca ) ), outerTarArchiveOutputStream );
                    } else {
                        outputTarArchiveEntry.setSize(
                                Files.size( Paths.get( tmpInputFilePath ) )
                        );
                        outerTarArchiveOutputStream.putArchiveEntry( outputTarArchiveEntry );
                        IOUtils.copy( Files.newInputStream( Paths.get( tmpInputFilePath ) ), outerTarArchiveOutputStream );
                    }
                    Paths.get( tmpInputFilePath ).toFile().delete();
                    outputTarArchiveEntry.setSize( outerTarArchiveOutputStream.getBytesWritten() );
                    outerTarArchiveOutputStream.closeArchiveEntry();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (outerInputTarArchiveEntry != null) {
                System.out.println( "tar file handling ended : " + outerInputTarArchiveEntry.getName() );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return changed;
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


    public static void handleDirectory(
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @NotNull TarArchiveEntry inputTarArchiveEntry,
            @NotNull TarArchiveOutputStream tarArchiveOutputStream
    ) {
        System.out.println( "directory : " + inputTarArchiveEntry.getName() );
        try {
            TarArchiveEntry outputTarArchiveEntry = new TarArchiveEntry(
                    inputTarArchiveEntry.getName(),
                    getLinkByte( inputTarArchiveEntry )
            );
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

            tarArchiveOutputStream.putArchiveEntry( outputTarArchiveEntry );
            IOUtils.copy( outerTarArchiveInputStream, tarArchiveOutputStream );
            outputTarArchiveEntry.setSize( tarArchiveOutputStream.getBytesWritten() );
            tarArchiveOutputStream.closeArchiveEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static byte getLinkByte(
            @NotNull TarArchiveEntry inputTarArchiveEntry
    ) {
        try {
            return inputTarArchiveEntry.getLinkFlag();
        } catch (Throwable ignored) {
            System.out.println( "no home-made getLinkByte, use original instead" );
            if (inputTarArchiveEntry.isLink()) {
                return TarConstants.LF_LINK;
            }
            if (inputTarArchiveEntry.isSymbolicLink()) {
                return TarConstants.LF_SYMLINK;
            }
            if (inputTarArchiveEntry.isGNULongLinkEntry()) {
                return TarConstants.LF_GNUTYPE_LONGLINK;
            }
            if (inputTarArchiveEntry.isDirectory()) {
                return TarConstants.LF_DIR;
            }
            return TarConstants.LF_NORMAL;
        }
    }

    public static void handleLinkFile(
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @NotNull TarArchiveEntry inputTarArchiveEntry,
            @NotNull TarArchiveOutputStream tarArchiveOutputStream
    ) {
        System.out.println( "special file : " + inputTarArchiveEntry.getName() );
        try {
            TarArchiveEntry outputTarArchiveEntry = new TarArchiveEntry(
                    inputTarArchiveEntry.getName(),
                    getLinkByte( inputTarArchiveEntry )
            );
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

            tarArchiveOutputStream.putArchiveEntry( outputTarArchiveEntry );
            IOUtils.copy( outerTarArchiveInputStream, tarArchiveOutputStream );
            outputTarArchiveEntry.setSize( tarArchiveOutputStream.getBytesWritten() );
            tarArchiveOutputStream.closeArchiveEntry();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean handleNormalFile(
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @NotNull TarArchiveEntry inputTarArchiveEntry,
            @NotNull TarArchiveOutputStream tarArchiveOutputStream,
            @NotNull TarArchiveOutputStream rootOuterTarArchiveOutputStream,
            @NotNull Map<String, FrontSearchResultPojo> frontSearchResult,
            @NotNull Map<String, File> tempDuplicatedFiles
    ) {
        System.out.println( "normal file : " + inputTarArchiveEntry.getName() );
        TarArchiveEntry outputTarArchiveEntry = new TarArchiveEntry(
                inputTarArchiveEntry.getName(),
                getLinkByte( inputTarArchiveEntry )
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
            boolean rebeccaPie;
            if (!frontSearchResult.containsKey( hash )) {
                rebeccaPie = false;
            } else {
                rebeccaPie = handleHashFiles(
                        rootOuterTarArchiveOutputStream,
                        readAndHashResultPojo,
                        tempDuplicatedFiles
                );
            }
            if (rebeccaPie) {
                outputTarArchiveEntry.setName( outputTarArchiveEntry.getName() + ".rebecca_pie" );
                byte[] hashStringBytes = readAndHashResultPojo.getHash().getBytes( StandardCharsets.UTF_8 );
                outputTarArchiveEntry.setSize( hashStringBytes.length );
                tarArchiveOutputStream.putArchiveEntry( outputTarArchiveEntry );
                tarArchiveOutputStream.write(
                        hashStringBytes
                );
                outputTarArchiveEntry.setSize( tarArchiveOutputStream.getBytesWritten() );
                tarArchiveOutputStream.closeArchiveEntry();
                return true;
            } else {
                tarArchiveOutputStream.putArchiveEntry( outputTarArchiveEntry );
                try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(readAndHashResultPojo.getData())) {
                    IOUtils.copy( byteArrayInputStream, tarArchiveOutputStream );
                }
                outputTarArchiveEntry.setSize( tarArchiveOutputStream.getBytesWritten() );
                tarArchiveOutputStream.closeArchiveEntry();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static boolean handleHashFiles(
            @NotNull TarArchiveOutputStream rootOuterTarArchiveOutputStream,
            @NotNull ReadAndHashResultPojo readAndHashResultPojo,
            @NotNull Map<String, File> tempDuplicatedFiles
    ) throws IOException {
        File file = tempDuplicatedFiles.get( readAndHashResultPojo.getHash() );
        if (file == null) {
            file = File.createTempFile( "rebecca-", ".encode" );
            file.deleteOnExit();
            FileUtils.writeByteArrayToFile( file, readAndHashResultPojo.getData() );
            tempDuplicatedFiles.put( readAndHashResultPojo.getHash(), file );
            TarArchiveEntry fileTarArchiveEntry = new TarArchiveEntry("hash_files/" + readAndHashResultPojo.getHash());
            fileTarArchiveEntry.setSize( readAndHashResultPojo.getData().length );
            fileTarArchiveEntry.setCreationTime( FileTime.fromMillis( 0 ) );
//                    outputTarArchiveEntry.setDevMajor(outerInputTarArchiveEntry.getDevMajor());
//                    outputTarArchiveEntry.setDevMinor(outerInputTarArchiveEntry.getDevMinor());
//                    outputTarArchiveEntry.setGroupId(outerInputTarArchiveEntry.getLongGroupId());
            fileTarArchiveEntry.setLastAccessTime( FileTime.fromMillis( 0 ) );
            fileTarArchiveEntry.setLastModifiedTime( FileTime.fromMillis( 0 ) );
//                    outputTarArchiveEntry.setLinkName(outerInputTarArchiveEntry.getLinkName());
//                    outputTarArchiveEntry.setMode(outerInputTarArchiveEntry.getMode());
            fileTarArchiveEntry.setModTime( FileTime.fromMillis( 0 ) );
//                    outputTarArchiveEntry.setStatusChangeTime(outerInputTarArchiveEntry.getStatusChangeTime());
            fileTarArchiveEntry.setUserId( 0 );
//                    outputTarArchiveEntry.setUserName(outerInputTarArchiveEntry.getUserName());
            rootOuterTarArchiveOutputStream.putArchiveEntry( fileTarArchiveEntry );
            rootOuterTarArchiveOutputStream.write( readAndHashResultPojo.getData() );
            fileTarArchiveEntry.setSize( rootOuterTarArchiveOutputStream.getBytesWritten() );
            rootOuterTarArchiveOutputStream.closeArchiveEntry();
            return true;
        } else {
            return Arrays.equals(
                    FileUtils.readFileToByteArray( file ),
                    readAndHashResultPojo.getData()
            );
        }
    }

    private Encoder() {
    }

}
