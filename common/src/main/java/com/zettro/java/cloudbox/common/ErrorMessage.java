package com.zettro.java.cloudbox.common;

public class ErrorMessage extends AbstractMessage {

    private static final long serialVersionUID = 6904554960775781110L;

    private final String message;

    public ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
