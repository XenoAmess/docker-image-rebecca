package com.xenoamess.docker.image.rebecca.pojo;

import java.io.File;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

public class FrontHashFilesPreparePojo {

    @NotNull
    private File tempHashFile;

    public FrontHashFilesPreparePojo(@NotNull File tempHashFile) {
        this.tempHashFile = tempHashFile;
    }

    @NotNull
    public File getTempHashFile() {
        return tempHashFile;
    }

    public void setTempHashFile(@NotNull File tempHashFile) {
        this.tempHashFile = tempHashFile;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        FrontHashFilesPreparePojo that = (FrontHashFilesPreparePojo) o;
        return getTempHashFile().equals( that.getTempHashFile() );
    }

    @Override
    public int hashCode() {
        return Objects.hash( getTempHashFile() );
    }
}
