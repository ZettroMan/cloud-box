package com.zettro.java.cloudbox.client;

import java.io.Serializable;

public class ProgramSettings implements Serializable {

    private static final long serialVersionUID = 982135885364460538L;

    private final String username;
    private final String password;
    private final String serverAddress;
    private final String serverPort;

    public ProgramSettings(String username, String password, String serverAddress, String serverPort) {
        this.username = username;
        this.password = password;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public String getServerPort() {
        return serverPort;
    }
}
