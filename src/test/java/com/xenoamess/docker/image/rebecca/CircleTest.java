package com.xenoamess.docker.image.rebecca;

import java.nio.file.Paths;

import com.xenoamess.docker.image.rebecca.decode.Decoder;
import com.xenoamess.docker.image.rebecca.encode.Encoder;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CircleTest {

    @Test
    public void test() throws Exception {
        Encoder.encode(
                "src/test/resources/1.tar",
                "target/out/1.tar.rebecca"
        );
        Decoder.decode(
                "target/out/1.tar.rebecca",
                "target/out/1_out.tar"
        );
    }

}
