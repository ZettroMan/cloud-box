package com.zettro.java.cloudbox.common;

public class TraverseToFolderMessage extends AbstractMessage {

    private static final long serialVersionUID = -9198673406781112615L;

    private final String folderName;

    public TraverseToFolderMessage(String folderName) {
        this.folderName = folderName;
    }

    public String getFoldername() {
        return folderName;
    }
}
