package com.xenoamess.docker.image.rebecca.decode;

import com.xenoamess.docker.image.rebecca.utils.TarCompareUtil;
import org.junit.jupiter.api.Test;

public class DecoderTest {

    @Test
    public void testOnLocal() throws Exception {
        Decoder.decode(
                "src/test/resources/decode0.tar.rebecca"
        );
        TarCompareUtil.assertTarEquals(
                "src/test/resources/decode0_expected.tar.rebecca.out.tar",
                "src/test/resources/decode0.tar.rebecca.out.tar"
        );
        Decoder.decode(
                "src/test/resources/decode1.tar.rebecca"
        );
        TarCompareUtil.assertTarEquals(
                "src/test/resources/decode1_expected.tar.rebecca.out.tar",
                "src/test/resources/decode1.tar.rebecca.out.tar"
        );
        Decoder.decode(
                "src/test/resources/decode1.tar.rebecca",
                "target/out/1_out.out.tar"
        );
        TarCompareUtil.assertTarEquals(
                "src/test/resources/decode1_expected.tar.rebecca.out.tar",
                "target/out/1_out.out.tar"
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
