package com.zettro.java.cloudbox.common;

public class AuthRequest extends AbstractMessage {

    private static final long serialVersionUID = 2081133089050492921L;

    private final String username;

    private final String password;

    public AuthRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
