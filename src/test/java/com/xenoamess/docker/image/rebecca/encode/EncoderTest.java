package com.xenoamess.docker.image.rebecca.encode;

import com.xenoamess.docker.image.rebecca.decode.Decoder;
import com.xenoamess.docker.image.rebecca.utils.TarCompareUtil;
import org.junit.jupiter.api.Test;

public class EncoderTest {

    @Test
    public void testOnLocal() throws Exception {
        Encoder.encode(
                "src/test/resources/0.tar"
        );
        TarCompareUtil.assertTarEquals(
                "src/test/resources/decode0.tar.rebecca",
                "src/test/resources/0.tar.rebecca"
        );
        Encoder.encode(
                "src/test/resources/1.tar"
        );
        TarCompareUtil.assertTarEquals(
                "src/test/resources/decode1.tar.rebecca",
                "src/test/resources/1.tar.rebecca"
        );
        Encoder.encode(
                "src/test/resources/1.tar",
                "target/out/1_out.tar"
        );
        TarCompareUtil.assertTarEquals(
                "src/test/resources/decode1.tar.rebecca",
                "target/out/1_out.tar"
        );
    }

    @Test
    public void test() throws Exception {
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

    @Test
    public void testLink() throws Exception {
        Encoder.encode(
                "src/test/resources/link.tar"
        );
        Decoder.decode(
                "src/test/resources/link.tar.rebecca"
        );
        Encoder.encode(
                "src/test/resources/link2.tar"
        );
        Decoder.decode(
                "src/test/resources/link2.tar.rebecca"
        );
    }

}
