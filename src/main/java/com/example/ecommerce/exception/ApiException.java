package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

/**
 * Base runtime exception carrying an HTTP status and a stable
 * machine-readable error code, so the GlobalExceptionHandler can build a
 * structured JSON error response without guessing.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorCode errorCode;

    public ApiException(HttpStatus status, ErrorCode errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
