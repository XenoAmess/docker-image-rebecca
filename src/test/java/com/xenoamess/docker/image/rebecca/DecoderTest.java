package com.xenoamess.docker.image.rebecca;

import com.xenoamess.docker.image.rebecca.decode.Decoder;
import com.xenoamess.docker.image.rebecca.encode.Encoder;
import org.junit.jupiter.api.Test;

public class DecoderTest {

    @Test
    public void test() {
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