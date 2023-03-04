package com.xenoamess.docker.image.rebecca;

import com.xenoamess.docker.image.rebecca.encode.Encoder;
import org.junit.jupiter.api.Test;

public class EncoderTest {

    @Test
    public void test() {
        Encoder.encode(
                "src/test/resources/0.tar"
        );
        Encoder.encode(
                "src/test/resources/1.tar"
        );
        Encoder.encode(
                "src/test/resources/1.tar",
                "target/out/1_out.tar"
        );
    }

}