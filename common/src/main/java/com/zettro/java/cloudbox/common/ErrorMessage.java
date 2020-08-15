package com.zettro.java.cloudbox.common;

public class ErrorMessage extends AbstractMessage {
    private final String message;

    public ErrorMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
