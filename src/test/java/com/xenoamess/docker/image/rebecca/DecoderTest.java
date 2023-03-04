package com.xenoamess.docker.image.rebecca;

import java.nio.file.Paths;

import com.xenoamess.docker.image.rebecca.decode.Decoder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DecoderTest {

    @Test
    public void test() throws Exception {
        Decoder.decode(
                "src/test/resources/decode0.tar.rebecca"
        );
        Assertions.assertArrayEquals(
                FileUtils.readFileToByteArray(
                        Paths.get("src/test/resources/decode0_expected.tar.rebecca.out.tar").toFile()
                ),
                FileUtils.readFileToByteArray(
                        Paths.get("src/test/resources/decode0.tar.rebecca.out.tar").toFile()
                )
        );
        Decoder.decode(
                "src/test/resources/decode1.tar.rebecca"
        );
        Assertions.assertArrayEquals(
                FileUtils.readFileToByteArray(
                        Paths.get("src/test/resources/decode1_expected.tar.rebecca.out.tar").toFile()
                ),
                FileUtils.readFileToByteArray(
                        Paths.get("src/test/resources/decode1.tar.rebecca.out.tar").toFile()
                )
        );
        Decoder.decode(
                "src/test/resources/decode1.tar.rebecca",
                "target/out/1_out.out.tar"
        );
        Assertions.assertArrayEquals(
                FileUtils.readFileToByteArray(
                        Paths.get("src/test/resources/decode1_expected.tar.rebecca.out.tar").toFile()
                ),
                FileUtils.readFileToByteArray(
                        Paths.get("target/out/1_out.out.tar").toFile()
                )
        );
    }

}