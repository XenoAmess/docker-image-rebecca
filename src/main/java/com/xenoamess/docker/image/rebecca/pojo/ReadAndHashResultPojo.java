package com.xenoamess.docker.image.rebecca.pojo;

import java.util.Arrays;

import org.jetbrains.annotations.NotNull;

public class ReadAndHashResultPojo {

    private byte @NotNull [] data;

    @NotNull
    private String hash;

    public ReadAndHashResultPojo(
            byte @NotNull [] data,
            @NotNull String hash
    ) {
        this.data = data;
        this.hash = hash;
    }

    public byte @NotNull [] getData() {
        return data;
    }

    public void setData(byte @NotNull [] data) {
        this.data = data;
    }

    @NotNull
    public String getHash() {
        return hash;
    }

    public void setHash(@NotNull String hash) {
        this.hash = hash;
    }

    @Override
    public String toString() {
        return "ReadAndHashResultPojo{" +
                "data=" + Arrays.toString(data) +
                ", hash='" + hash + '\'' +
                '}';
    }
}
