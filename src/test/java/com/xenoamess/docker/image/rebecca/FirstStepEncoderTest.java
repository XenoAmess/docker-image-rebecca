package com.xenoamess.docker.image.rebecca;

import com.xenoamess.docker.image.rebecca.encode.FirstStepEncoder;
import org.junit.jupiter.api.Test;

public class FirstStepEncoderTest {

    @Test
    public void test() {
        FirstStepEncoder.encodeFirstStep(
                "src/test/resources/0.tar"
        );
        FirstStepEncoder.encodeFirstStep(
                "src/test/resources/0.tar",
                "target/out/0_out.tar"
        );
    }

}