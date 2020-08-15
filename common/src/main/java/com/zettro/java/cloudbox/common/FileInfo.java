package com.zettro.java.cloudbox.common;

import java.io.Serializable;

public class FileInfo implements Serializable {
    private static final long serialVersionUID = 2008048922391800818L;

    String filename;

    public FileInfo(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}
