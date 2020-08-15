package com.zettro.java.cloudbox.common;

public class AuthAnswer extends AbstractMessage {
    public enum AuthResult {PASSED, FAILED}

    AuthResult authResult;

    public AuthAnswer(AuthResult authResult) {
        this.authResult = authResult;
    }

    public AuthResult getAuthResult() {
        return authResult;
    }
}
