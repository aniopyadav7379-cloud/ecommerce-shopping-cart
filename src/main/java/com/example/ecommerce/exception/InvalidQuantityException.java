package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class InvalidQuantityException extends ApiException {
    public InvalidQuantityException(String message) {
        super(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_QUANTITY, message);
    }
}
