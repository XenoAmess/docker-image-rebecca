package com.xenoamess.docker.image.rebecca.cmd;

import java.util.Collection;

import com.xenoamess.docker.image.rebecca.decode.Decoder;
import com.xenoamess.docker.image.rebecca.encode.Encoder;
import com.xenoamess.docker.image.rebecca.scan.Scanner;
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
            case "--encode":
                if (args.length >= 4) {
                    encode(
                            args[1],
                            args[2],
                            args[3]
                    );
                } else if (args.length >= 3) {
                    encode(
                            args[1],
                            args[2],
                            null
                    );
                } else {
                    encode(
                            args[1],
                            null,
                            null
                    );
                }
                break;
            case "decode":
            case "-decode":
            case "--decode":
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
            case "scan":
            case "-scan":
            case "--scan":
                if (args.length >= 3) {
                    scan(
                            args[1],
                            args[2]
                    );
                } else {
                    scan(
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
            @Nullable String outputFilePath,
            @Nullable String fileNameFilterRegexString
    ) {
        System.out.println( "inputFilePath : " + inputFilePath );
        System.out.println( "outputFilePath : " + outputFilePath );
        System.out.println( "fileNameFilterRegexString : " + fileNameFilterRegexString );
        Encoder.encode(
                inputFilePath,
                outputFilePath,
                fileNameFilterRegexString
        );
    }

    public static void decode(
            @NotNull String inputFilePath,
            @Nullable String outputFilePath
    ) {
        System.out.println( "inputFilePath : " + inputFilePath );
        System.out.println( "outputFilePath : " + outputFilePath );
        Decoder.decode(
                inputFilePath,
                outputFilePath
        );
    }

    public static void scan(
            @NotNull String inputFileOrFolderPath,
            @Nullable String fileNameFilterRegexString
    ) {
        System.out.println( "inputFileOrFolderPath : " + inputFileOrFolderPath );
        System.out.println( "fileNameFilterRegexString : " + fileNameFilterRegexString );
        @NotNull Collection<String> result = Scanner.scan(
                inputFileOrFolderPath,
                fileNameFilterRegexString
        );
        System.out.println( "scan final matched files : " );
        for (String fileName : result) {
            System.out.println( fileName );
        }
    }

}
