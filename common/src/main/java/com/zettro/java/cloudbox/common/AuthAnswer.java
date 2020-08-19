package com.zettro.java.cloudbox.common;

public class AuthAnswer extends AbstractMessage {
    private static final long serialVersionUID = 3962985536550394922L;

    public enum AuthResult {PASSED, FAILED}

    AuthResult authResult;
    String message;

    public AuthAnswer(AuthResult authResult, String message) {
        this.authResult = authResult;
        this.message = message;
    }

    public AuthResult getAuthResult() {
        return authResult;
    }

    public String getMessage() {
        return message;
    }
}
