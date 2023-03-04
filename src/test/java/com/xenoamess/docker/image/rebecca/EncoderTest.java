package com.xenoamess.docker.image.rebecca;

import java.nio.file.Paths;

import com.xenoamess.docker.image.rebecca.encode.Encoder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EncoderTest {

    @Test
    public void test() throws Exception {
        if(!SystemUtils.IS_OS_WINDOWS){
            return;
        }
        Encoder.encode(
                "src/test/resources/0.tar"
        );
        Assertions.assertArrayEquals(
                FileUtils.readFileToByteArray(
                        Paths.get( "src/test/resources/decode0.tar.rebecca" ).toFile()
                ),
                FileUtils.readFileToByteArray(
                        Paths.get( "src/test/resources/0.tar.rebecca" ).toFile()
                )
        );
        Encoder.encode(
                "src/test/resources/1.tar"
        );
        Assertions.assertArrayEquals(
                FileUtils.readFileToByteArray(
                        Paths.get( "src/test/resources/decode1.tar.rebecca" ).toFile()
                ),
                FileUtils.readFileToByteArray(
                        Paths.get( "src/test/resources/1.tar.rebecca" ).toFile()
                )
        );
        Encoder.encode(
                "src/test/resources/1.tar",
                "target/out/1_out.tar"
        );
        Assertions.assertArrayEquals(
                FileUtils.readFileToByteArray(
                        Paths.get( "src/test/resources/decode1.tar.rebecca" ).toFile()
                ),
                FileUtils.readFileToByteArray(
                        Paths.get( "target/out/1_out.tar" ).toFile()
                )
        );
    }

}
