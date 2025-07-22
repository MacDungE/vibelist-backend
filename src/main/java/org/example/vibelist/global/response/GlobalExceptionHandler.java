package org.example.vibelist.global.response;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(GlobalException.class)
    public ResponseEntity<RsData<?>> handleGlobalException(GlobalException e) {
        ResponseCode code = e.getResponseCode();
        String message = e.getCustomMessage() != null ? e.getCustomMessage() : code.getMessage();
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(RsData.fail(code, message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RsData<?>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity
                .badRequest()
                .body(RsData.fail(ResponseCode.BAD_REQUEST, message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RsData<?>> handleAllUncaughtException(Exception e) {
        return ResponseEntity
                .status(ResponseCode.INTERNAL_SERVER_ERROR.getHttpStatus())
                .body(RsData.fail(ResponseCode.INTERNAL_SERVER_ERROR, e.getMessage()));
    }
}