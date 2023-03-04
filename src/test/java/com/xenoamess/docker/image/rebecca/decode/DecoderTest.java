package com.xenoamess.docker.image.rebecca.decode;

import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class DecoderTest {

    @Disabled
    @Test
    public void testOnLocal() throws Exception {
        Decoder.decode(
                "src/test/resources/decode0.tar.rebecca"
        );
        Assertions.assertArrayEquals(
                FileUtils.readFileToByteArray(
                        Paths.get( "src/test/resources/decode0_expected.tar.rebecca.out.tar" ).toFile()
                ),
                FileUtils.readFileToByteArray(
                        Paths.get( "src/test/resources/decode0.tar.rebecca.out.tar" ).toFile()
                )
        );
        Decoder.decode(
                "src/test/resources/decode1.tar.rebecca"
        );
        Assertions.assertArrayEquals(
                FileUtils.readFileToByteArray(
                        Paths.get( "src/test/resources/decode1_expected.tar.rebecca.out.tar" ).toFile()
                ),
                FileUtils.readFileToByteArray(
                        Paths.get( "src/test/resources/decode1.tar.rebecca.out.tar" ).toFile()
                )
        );
        Decoder.decode(
                "src/test/resources/decode1.tar.rebecca",
                "target/out/1_out.out.tar"
        );
        Assertions.assertArrayEquals(
                FileUtils.readFileToByteArray(
                        Paths.get( "src/test/resources/decode1_expected.tar.rebecca.out.tar" ).toFile()
                ),
                FileUtils.readFileToByteArray(
                        Paths.get( "target/out/1_out.out.tar" ).toFile()
                )
        );
    }

    @Test
    public void test() throws Exception {
        Decoder.decode(
                "src/test/resources/decode0.tar.rebecca"
        );
        Decoder.decode(
                "src/test/resources/decode1.tar.rebecca"
        );
        Decoder.decode(
                "src/test/resources/decode1.tar.rebecca",
                "target/out/1_out.out.tar"
        );
    }

}