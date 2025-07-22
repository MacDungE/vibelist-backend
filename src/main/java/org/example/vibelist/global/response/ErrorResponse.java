package org.example.vibelist.global.response;

import java.time.LocalDateTime;

/**
 * @deprecated RsData.fail(code, message)로 대체하세요.
 */
@Deprecated
public class ErrorResponse {

    private final String code;
    private final String message;
    private final LocalDateTime timestamp;

    public ErrorResponse(String code, String message) {
        this.code = code;
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}