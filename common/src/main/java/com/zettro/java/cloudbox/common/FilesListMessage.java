package com.zettro.java.cloudbox.common;

import java.util.ArrayList;

public class FilesListMessage extends AbstractMessage {

    private static final long serialVersionUID = -4064258077443075133L;

    private final ArrayList<FileInfo> files;

    public FilesListMessage() {
        files = new ArrayList<>();
    }

    public ArrayList<FileInfo> getFiles() {
        return files;
    }

    public void addFileInfo(FileInfo fi) {
        files.add(fi);
    }
}
