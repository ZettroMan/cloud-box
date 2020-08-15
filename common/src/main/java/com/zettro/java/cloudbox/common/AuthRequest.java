package com.zettro.java.cloudbox.common;

public class AuthRequest extends AbstractMessage {

    private static final long serialVersionUID = 2081133089050492921L;

    private final String username;

    public AuthRequest(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }
}
