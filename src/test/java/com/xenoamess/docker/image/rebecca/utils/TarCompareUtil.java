package com.xenoamess.docker.image.rebecca.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;

/**
 * from stackoverflow
 * <p>
 * {@see https://stackoverflow.com/questions/30738470/what-is-the-best-way-to-compare-tar-archives-in-junit-testing}
 */
public final class TarCompareUtil {

    public static final void assertArchiveEquals(String type, String archive1, String archive2) throws NoSuchAlgorithmException, IOException {
        if (type.endsWith( "zip" )) {
            assertZipEquals( archive1, archive2 );
        } else {
            assertTarEquals( archive1, archive2 );
        }
    }

    /**
     * @param archive1
     * @param archive2
     * @throws ZipException
     * @throws IOException
     */
    public static final void assertZipEquals(String archive1, String archive2) throws ZipException, IOException {
        // Get Archives
        ZipFile zipFile1 = new ZipFile(new File(archive1));
        ZipFile zipFile2 = new ZipFile(new File(archive2));

        // Get Member Hash
        HashMap<String, ZipEntry> files1 = getMembers( zipFile1 );
        HashMap<String, ZipEntry> files2 = getMembers( zipFile2 );

        // Compare Files
        assertMembersEqual( zipFile1, files1, zipFile2, files2 );
    }

    /**
     * @param archive
     * @return
     * @throws IOException
     */
    private static final HashMap<String, ZipEntry> getMembers(ZipFile archive) throws IOException {
        HashMap<String, ZipEntry> map = new HashMap<String, ZipEntry>();
        @SuppressWarnings("unchecked")
        Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) archive.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            map.put( entry.getName(), entry );
        }
        return map;
    }

    /**
     * @param zip1
     * @param files1
     * @param zip2
     * @param files2
     * @throws IOException
     */
    private static final void assertMembersEqual(ZipFile zip1, HashMap<String, ZipEntry> files1,
                                                 ZipFile zip2, HashMap<String, ZipEntry> files2) throws IOException {
        if (files1.size() != files2.size()) {
            Assertions.fail( "Different Sizes, expected " + Integer.toString( files1.size() ) + " found " + Integer.toString( files2.size() ) );
        }

        for (String key : files1.keySet()) {
            if (!files2.containsKey( key )) {
                Assertions.fail( "Expected file not in target " + key );
            }
            String file1 = IOUtils.toString( zip1.getInputStream( files1.get( key ) ) );
            String file2 = IOUtils.toString( zip2.getInputStream( files2.get( key ) ) );
            Assertions.assertEquals( file1, file2 );
        }
    }

    /**
     * @param archive1
     * @param archive2
     * @throws IOException
     * @throws NoSuchAlgorithmException
     */
    public static final void assertTarEquals(String archive1, String archive2) throws IOException, NoSuchAlgorithmException {
        // Get Member Hash
        HashMap<String, TarArchiveEntry> files1 = getMembers( archive1 );
        HashMap<String, TarArchiveEntry> files2 = getMembers( archive2 );

        // Compare Files
        assertMembersEqual( archive1, files1, archive2, files2 );
    }

    /**
     * @param archive
     * @return
     * @throws IOException
     */
    private static final HashMap<String, TarArchiveEntry> getMembers(String archive) throws IOException {
        TarArchiveInputStream input = new TarArchiveInputStream(
                new BufferedInputStream(
                        new FileInputStream(archive)));

        TarArchiveEntry entry;
        HashMap<String, TarArchiveEntry> map = new HashMap<String, TarArchiveEntry>();
        while ((entry = input.getNextTarEntry()) != null) {
            map.put( entry.getName(), entry );
        }
        input.close();
        return map;
    }

    /**
     * @param tar1
     * @param files1
     * @param tar2
     * @param files2
     * @throws IOException
     */
    private static final void assertMembersEqual(String tar1, HashMap<String, TarArchiveEntry> files1,
                                                 String tar2, HashMap<String, TarArchiveEntry> files2) throws IOException {
        if (files1.size() != files2.size()) {
            Assertions.fail( "Different Sizes, expected " + Integer.toString( files1.size() ) + " found " + Integer.toString( files2.size() ) );
        }

        for (String key : files1.keySet()) {
            if (!files2.containsKey( key )) {
                Assertions.fail( "Expected file not in target " + key );
            }
        }

        for (String key : files1.keySet()) {
            if (!files2.containsKey( key )) {
                Assertions.fail( "Expected file not in target " + key );
            }
        }
        for (String key : files1.keySet()) {
            String file1 = getTarFile( tar1, key );
            String file2 = getTarFile( tar2, key );
            Assertions.assertEquals( file1, file2 );
        }
    }

    /**
     * @param archive
     * @param name
     * @return
     * @throws IOException
     */
    private static final String getTarFile(String archive, String name) throws IOException {
        TarArchiveInputStream input = new TarArchiveInputStream(
                new BufferedInputStream(
                        new FileInputStream(archive)));
        TarArchiveEntry entry;
        while ((entry = input.getNextTarEntry()) != null) {
            if (entry.getName().equals( name )) {
                byte[] content = new byte[(int) entry.getSize()];
                input.read( content, 0, content.length );
                input.close();
                return new String(content);
            }
        }
        input.close();
        return "";
    }

    private TarCompareUtil() {
    }

}
