package com.zettro.java.cloudbox.common;

public class FileRequest extends AbstractMessage {

    private static final long serialVersionUID = 5966548232236868213L;
    public enum ActionType {
        DOWNLOAD, DELETE, CREATE_DIR}

    private final String filename;
    ActionType actionType;

    public String getFileName() {
        return filename;
    }
    public ActionType getActionType() {
        return actionType;
    }

    public FileRequest(String filename, ActionType actionType)
    {
        this.filename = filename;
        this.actionType = actionType;
    }
}
