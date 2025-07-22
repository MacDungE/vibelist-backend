package org.example.vibelist.global.response;

public class GlobalException extends RuntimeException {

    private final ResponseCode responseCode;
    private final String customMessage;

    public GlobalException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
        this.customMessage = null;
    }

    public GlobalException(ResponseCode responseCode, String customMessage) {
        super(customMessage != null ? customMessage : responseCode.getMessage());
        this.responseCode = responseCode;
        this.customMessage = customMessage;
    }

    public ResponseCode getResponseCode() {
        return responseCode;
    }

    public String getCustomMessage() {
        return customMessage;
    }
}