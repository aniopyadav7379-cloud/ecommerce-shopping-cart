package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends ApiException {
    public InsufficientStockException(String productName, int available) {
        super(HttpStatus.CONFLICT, ErrorCode.INSUFFICIENT_STOCK,
                "Only " + available + " unit(s) of \"" + productName + "\" are available.");
    }
}
