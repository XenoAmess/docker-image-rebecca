package com.xenoamess.docker.image.rebecca.pojo;

import java.util.HashSet;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public class FrontSearchResultPojo {

    @NotNull
    private final HashSet<String> fileNames = new HashSet<>();

    @NotNull
    private final HashSet<Long> fileSizes = new HashSet<>();

    private int count;

    @NotNull
    public HashSet<String> getFileNames() {
        return fileNames;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @NotNull
    public  HashSet<Long> getFileSizes() {
        return fileSizes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FrontSearchResultPojo that = (FrontSearchResultPojo) o;
        return getCount() == that.getCount() && getFileNames().equals( that.getFileNames() ) && getFileSizes().equals( that.getFileSizes() );
    }

    @Override
    public int hashCode() {
        return Objects.hash( getFileNames(), getFileSizes(), getCount() );
    }
}
