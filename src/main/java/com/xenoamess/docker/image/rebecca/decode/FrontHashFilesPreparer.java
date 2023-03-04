package com.xenoamess.docker.image.rebecca.decode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import com.xenoamess.docker.image.rebecca.pojo.FrontHashFilesPreparePojo;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

public class FrontHashFilesPreparer {

    @NotNull
    public static Map<String, FrontHashFilesPreparePojo> frontHashFilesPrepare(
            @NotNull String inputFilePath
    ) {
        final Map<String, FrontHashFilesPreparePojo> result = new HashMap<>();
        try (
                InputStream inputStream = Files.newInputStream(Paths.get(inputFilePath));
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(bufferedInputStream);
        ) {
            handleTarFile(
                    tarArchiveInputStream,
                    result
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static void handleTarFile(
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @NotNull Map<String, FrontHashFilesPreparePojo> result
    ) {
        try {
            while (true) {
                TarArchiveEntry inputTarArchiveEntry = outerTarArchiveInputStream.getNextTarEntry();
                if (inputTarArchiveEntry == null) {
                    break;
                }
                handleHashFile(
                        outerTarArchiveInputStream,
                        inputTarArchiveEntry,
                        result
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleHashFile(
            @NotNull TarArchiveInputStream outerTarArchiveInputStream,
            @NotNull TarArchiveEntry inputTarArchiveEntry,
            @NotNull Map<String, FrontHashFilesPreparePojo> result
    ) {
        System.out.println("normal file : " + inputTarArchiveEntry.getName());
        if (!inputTarArchiveEntry.getName().startsWith("hash_files/")) {
            return;
        }
        String hash = inputTarArchiveEntry.getName().substring("hash_files/".length());
        try {
            File tempFile = File.createTempFile("rebecca-", ".decode");
            tempFile.deleteOnExit();
            try (
                    FileOutputStream outputStream = new FileOutputStream(tempFile);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream);
            ) {
                IOUtils.copy(
                        outerTarArchiveInputStream,
                        bufferedOutputStream
                );
                result.put(
                        hash,
                        new FrontHashFilesPreparePojo(
                                tempFile
                        )
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
