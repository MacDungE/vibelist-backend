package org.example.vibelist.global.response;

import java.time.LocalDateTime;

public class RsData<T> {
    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    private final LocalDateTime timestamp;

    private RsData(boolean success, ResponseCode code, String message, T data) {
        this.success = success;
        this.code = code.getCode();
        this.message = message != null ? message : code.getMessage();
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public static <T> RsData<T> success(ResponseCode code, T data) {
        return new RsData<>(true, code, code.getMessage(), data);
    }

    public static <T> RsData<T> success(ResponseCode code, String message, T data) {
        return new RsData<>(true, code, message, data);
    }

    public static <T> RsData<T> fail(ResponseCode code) {
        return new RsData<>(false, code, code.getMessage(), null);
    }

    public static <T> RsData<T> fail(ResponseCode code, String message) {
        return new RsData<>(false, code, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
} 