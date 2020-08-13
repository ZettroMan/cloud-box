package com.zettro.java.cloudbox.common;

public class AuthAnswer extends AbstractMessage {
    private static final long serialVersionUID = 3962985536550394922L;

    public enum AuthResult {PASSED, FAILED}

    AuthResult authResult;

    public AuthAnswer(AuthResult authResult) {
        this.authResult = authResult;
    }

    public AuthResult getAuthResult() {
        return authResult;
    }
}
