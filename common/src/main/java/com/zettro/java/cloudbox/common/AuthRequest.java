package com.zettro.java.cloudbox.common;

public class AuthRequest extends AbstractMessage {

    private static final long serialVersionUID = 2081133089050492921L;

    private final String username;
    private final String password;
    private final boolean newUser;

    public AuthRequest(String username, String password, boolean newUser) {
        this.username = username;
        this.password = password;
        this.newUser = newUser;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isNewUser() {
        return newUser;
    }
}
