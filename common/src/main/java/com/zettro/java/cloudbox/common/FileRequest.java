package com.zettro.java.cloudbox.common;

public class FileRequest extends AbstractMessage {

    private static final long serialVersionUID = 5966548232236868213L;

    private final String filename;

    public String getFileName() {
        return filename;
    }

    public FileRequest(String filename) {
        this.filename = filename;
    }
}
