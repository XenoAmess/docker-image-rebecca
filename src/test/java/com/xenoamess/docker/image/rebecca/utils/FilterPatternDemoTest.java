package com.xenoamess.docker.image.rebecca.utils;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.xenoamess.docker.image.rebecca.encode.Encoder;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

public class FilterPatternDemoTest {

    private static final List<String> SUB_PATTERN_STRINGS = Arrays.asList(
            //backend
            "^.*\\.jar$",
            "^.*\\.py$",
            //frontend
            "^.*\\.html$",
            "^.*\\.css$",
            "^.*\\.js$",
            "^.*\\.ts$",
            //config
            "^.*\\.conf$",
            "^.*\\.ini$",
            //zip
            "^.*\\.zip$",
            "^.*\\.gz$",
            "^.*\\.bz$",
            "^.*\\.bz2$",
            //text
            "^.*\\.md$",
            "^.*\\.txt$",
            //data
            "^.*\\.json$",
            "^.*\\.dtd$",
            "^.*\\.xml$",
            "^.*\\.properties$",
            "^.*\\.dat$",
            "^.*\\.data$",
            //image
            "^.*\\.ttf$",
            "^.*\\.jpg$",
            "^.*\\.bpm$",
            //doc
            "^.*\\.doc$",
            "^.*\\.pdf$",
            //license
            "^VERSION$",
            "^LICENSE$",
            "^ASSEMBLY_EXCEPTION$",
            "^ADDITIONAL_LICENSE_INFO$"
    );

    @Disabled
    @Test
    public void testOnLocal() throws Exception {
        List<String> list = new ArrayList<>(SUB_PATTERN_STRINGS);
        SUB_PATTERN_STRINGS.forEach(
                s -> {
                    list.add( s.toUpperCase() );
                }
        );
        String goodRegex = StringUtils.join( list, '|' );
        System.out.println( goodRegex );
        Encoder.encode(
                "src/test/resources/1.tar",
                "target/out/1_out.tar",
                goodRegex
        );
        Assertions.assertArrayEquals(
                FileUtils.readFileToByteArray(
                        Paths.get( "src/test/resources/decode1.tar.rebecca" ).toFile()
                ),
                FileUtils.readFileToByteArray(
                        Paths.get( "target/out/1_out.tar" ).toFile()
                )
        );

        Encoder.encode(
                "src/test/resources/1.tar",
                "target/out/fake_out.tar",
                "1|2|3"
        );
        Assertions.assertThrows(
                AssertionFailedError.class, () -> {
                    Assertions.assertArrayEquals(
                            FileUtils.readFileToByteArray(
                                    Paths.get( "src/test/resources/decode1.tar.rebecca" ).toFile()
                            ),
                            FileUtils.readFileToByteArray(
                                    Paths.get( "target/out/fake_out.tar" ).toFile()
                            )
                    );
                }
        );

    }

}
