package com.xenoamess.docker.image.rebecca.decode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

import com.xenoamess.docker.image.rebecca.pojo.FrontHashFilesPreparePojo;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.archivers.tar.TarFile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.xenoamess.docker.image.rebecca.encode.Encoder.getLinkByte;
import static com.xenoamess.docker.image.rebecca.encode.Encoder.handleDirectory;
import static com.xenoamess.docker.image.rebecca.encode.Encoder.handleLinkFile;
import static com.xenoamess.docker.image.rebecca.encode.FrontSearcher.isLinkFile;

public class Decoder {

    public static void decode(@NotNull String inputFilePath) {
        decode( inputFilePath, null );
    }

    public static void decode(
            @NotNull String inputFilePath,
            @Nullable String outputFilePath
    ) {
        Map<String, FrontHashFilesPreparePojo> frontHashFilesPrepareResult = FrontHashFilesPreparer.frontHashFilesPrepare( inputFilePath );
        try (
                TarFile outerTarFile = new TarFile(Paths.get( inputFilePath ));
                InputStream inputStream = outerTarFile.getInputStream(
                        outerTarFile.getEntries().stream().filter(
                                new Predicate<TarArchiveEntry>() {
                                    @Override
                                    public boolean test(TarArchiveEntry tarArchiveEntry) {
                                        return "origin.tar".equals( tarArchiveEntry.getName() );
                                    }
                                }
                        ).findFirst().orElseThrow(
                                () -> new IllegalArgumentException("have no origin.tar file, cannot decode")
                        )
                );
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream)
        ) {
            String outputFileRebecca;
            if (outputFilePath != null) {
                outputFileRebecca = outputFilePath;
            } else {
                outputFileRebecca = inputFilePath + ".out.tar";
            }
//            try (
//                    OutputStream outputStream = Files.newOutputStream(Paths.get(outputFileRebecca));
//                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
//                    TarArchiveOutputStream tarArchiveOutputStream = new TarArchiveOutputStream(bufferedOutputStream)
//            ) {
            Decoder.handleTarFile(
                    inputFilePath,
                    outputFileRebecca,
                    true,
                    bufferedInputStream,
                    null,
                    null,
                    frontHashFilesPrepareResult
            );
//            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (FrontHashFilesPreparePojo frontHashFilesPreparePojo : frontHashFilesPrepareResult.values()) {
            try {
                frontHashFilesPreparePojo.getTempHashFile().delete();
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
            @NotNull Map<String, FrontHashFilesPreparePojo> frontHashFilesPrepareResult
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
                                    frontHashFilesPrepareResult
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
                            frontHashFilesPrepareResult
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
                        outputTarArchiveEntry = new TarArchiveEntry(
                                 outerInputTarArchiveEntry.getName(),
                                getLinkByte( outerInputTarArchiveEntry )
                        );
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
//                    outputTarArchiveEntry.setUserId(outerInputTarArchiveEntry.getLongUserId());
//                    outputTarArchiveEntry.setUserName(outerInputTarArchiveEntry.getUserName());
                    }
                    outerTarArchiveOutputStream.putArchiveEntry( outputTarArchiveEntry );
                    if (changed) {
                        outputTarArchiveEntry.setSize(
                                Files.size( Paths.get( outputFileRebecca ) )
                        );
                        IOUtils.copy( Files.newInputStream( Paths.get( outputFileRebecca ) ), outerTarArchiveOutputStream );
                    } else {
                        outputTarArchiveEntry.setSize(
                                Files.size( Paths.get( tmpInputFilePath ) )
                        );
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

        } catch (Exception e){
            e.printStackTrace();
        }
        return changed;
    }

    private static boolean handleNormalFile(
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @NotNull TarArchiveEntry inputTarArchiveEntry,
            @NotNull TarArchiveOutputStream tarArchiveOutputStream,
            @NotNull Map<String, FrontHashFilesPreparePojo> frontHashFilesPrepareResult
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
            boolean rebeccaPie = inputTarArchiveEntry.getName().endsWith( ".rebecca_pie" );
            if (rebeccaPie) {
                outputTarArchiveEntry.setName( outputTarArchiveEntry.getName().substring( 0, outputTarArchiveEntry.getName().length() - ".rebecca_pie".length() ) );
                String hash;
                byte[] hashBytes;
                try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                    IOUtils.copy( outerTarArchiveInputStream, byteArrayOutputStream );
                    hashBytes = byteArrayOutputStream.toByteArray();
                    hash = new String(hashBytes, StandardCharsets.UTF_8);
                }
                FrontHashFilesPreparePojo frontHashFilesPreparePojo = frontHashFilesPrepareResult.get( hash );
                if (frontHashFilesPreparePojo == null) {
                    outputTarArchiveEntry.setName( inputTarArchiveEntry.getName() );
                    tarArchiveOutputStream.putArchiveEntry( outputTarArchiveEntry );
                    tarArchiveOutputStream.write( hashBytes );
                    outputTarArchiveEntry.setSize( tarArchiveOutputStream.getBytesWritten() );
                    tarArchiveOutputStream.closeArchiveEntry();
                    return false;
                } else {
                    File file = frontHashFilesPreparePojo.getTempHashFile();
                    byte[] bytes = FileUtils.readFileToByteArray( file );
                    outputTarArchiveEntry.setSize( bytes.length );
                    tarArchiveOutputStream.putArchiveEntry( outputTarArchiveEntry );
                    tarArchiveOutputStream.write( bytes );
                    tarArchiveOutputStream.closeArchiveEntry();
                    return true;
                }
            } else {
                tarArchiveOutputStream.putArchiveEntry( outputTarArchiveEntry );
                IOUtils.copy( outerTarArchiveInputStream, tarArchiveOutputStream );
                outputTarArchiveEntry.setSize( tarArchiveOutputStream.getBytesWritten() );
                tarArchiveOutputStream.closeArchiveEntry();
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private Decoder() {
    }

}
