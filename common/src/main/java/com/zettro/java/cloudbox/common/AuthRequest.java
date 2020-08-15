package com.zettro.java.cloudbox.common;

public class AuthRequest extends AbstractMessage {
    private final String username;

    public AuthRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
