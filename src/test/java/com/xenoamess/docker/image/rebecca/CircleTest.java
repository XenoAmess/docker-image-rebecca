package com.xenoamess.docker.image.rebecca;

import com.xenoamess.docker.image.rebecca.decode.Decoder;
import com.xenoamess.docker.image.rebecca.encode.Encoder;
import com.xenoamess.docker.image.rebecca.utils.TarCompareUtil;
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

    @Test
    public void depth3Test() throws Exception {
        Encoder.encode(
                "src/test/resources/1.tar",
                "target/out/1.tar.rebecca"
        );
        Encoder.encode(
                "src/test/resources/1.tar.rebecca",
                "target/out/1.tar.rebecca.rebecca"
        );
        Encoder.encode(
                "target/out/1.tar.rebecca.rebecca",
                "target/out/1.tar.rebecca.rebecca.rebecca"
        );
        Decoder.decode(
                "target/out/1.tar.rebecca.rebecca.rebecca",
                "target/out/1_out.tar.rebecca.rebecca"
        );
        TarCompareUtil.assertTarEquals(
                "target/out/1.tar.rebecca.rebecca",
                "target/out/1_out.tar.rebecca.rebecca"
        );
        Decoder.decode(
                "target/out/1_out.tar.rebecca.rebecca",
                "target/out/1_out.tar.rebecca"
        );
        TarCompareUtil.assertTarEquals(
                "target/out/1.tar.rebecca",
                "target/out/1_out.tar.rebecca"
        );
        Decoder.decode(
                "target/out/1_out.tar.rebecca",
                "target/out/1_out.tar"
        );
    }

}
