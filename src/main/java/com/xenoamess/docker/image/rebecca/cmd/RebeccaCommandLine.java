package com.xenoamess.docker.image.rebecca.cmd;

import com.xenoamess.docker.image.rebecca.decode.Decoder;
import com.xenoamess.docker.image.rebecca.encode.Encoder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RebeccaCommandLine {

    public static void main(String[] args) {
        if (args.length == 0) {
            printHelp();
            return;
        }
        switch (args[0]) {
            case "encode":
            case "-encode":
                if (args.length >= 3) {
                    encode(
                            args[1],
                            args[2]
                    );
                } else {
                    encode(
                            args[1],
                            null
                    );
                }
                break;
            case "decode":
            case "-decode":
                if (args.length >= 3) {
                    decode(
                            args[1],
                            args[2]
                    );
                } else {
                    decode(
                            args[1],
                            null
                    );
                }
                break;
            case "help":
            case "h":
            case "-help":
            case "-h":
            default:
                printHelp();
                break;
        }
    }

    public static void printHelp() {
        System.out.println( "Use it as:" );
        System.out.println( "  Encode with input and output:" );
        System.out.println( "    encode 1.tar 1.tar.rebecca" );
        System.out.println( "  Encode only input (auto name output):" );
        System.out.println( "    encode 1.tar" );
        System.out.println( "  Decode with input and output:" );
        System.out.println( "    decode 1.tar.rebecca 1.tar" );
        System.out.println( "  Decode only input (auto name output):" );
        System.out.println( "    decode 1.tar.rebecca" );
    }

    public static void encode(
            @NotNull String inputFilePath,
            @Nullable String outputFilePath
    ) {
        Encoder.encode(
                inputFilePath,
                outputFilePath
        );
    }

    public static void decode(
            @NotNull String inputFilePath,
            @Nullable String outputFilePath
    ) {
        Decoder.decode(
                inputFilePath,
                outputFilePath
        );
    }

}
