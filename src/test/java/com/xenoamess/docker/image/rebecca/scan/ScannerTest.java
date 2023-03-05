package com.xenoamess.docker.image.rebecca.scan;

import com.xenoamess.docker.image.rebecca.cmd.RebeccaCommandLine;
import org.junit.jupiter.api.Test;

public class ScannerTest {

    @Test
    public void test() throws Exception {
        System.out.println( "--------------------" );
        RebeccaCommandLine.scan(
                "src/test/resources/0.tar",
                null
        );
        System.out.println( "--------------------" );
        RebeccaCommandLine.scan(
                "src/test/resources",
                null
        );
        System.out.println( "--------------------" );
        RebeccaCommandLine.scan(
                "src/test/resources/",
                null
        );
        System.out.println( "--------------------" );
        RebeccaCommandLine.scan(
                "src/test/resources",
                "^.*txt$"
        );
        System.out.println( "--------------------" );
        RebeccaCommandLine.scan(
                "src/test/resources/",
                "^.*txt$"
        );
        System.out.println( "--------------------" );
        RebeccaCommandLine.scan(
                "src/test/resources",
                "^.*rap$"
        );
        System.out.println( "--------------------" );
        RebeccaCommandLine.scan(
                "src/test/resources/",
                "^.*rap$"
        );
        System.out.println( "--------------------" );
    }

}
