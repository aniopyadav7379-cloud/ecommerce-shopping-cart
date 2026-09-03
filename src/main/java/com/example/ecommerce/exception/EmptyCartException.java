package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class EmptyCartException extends ApiException {
    public EmptyCartException() {
        super(HttpStatus.BAD_REQUEST, ErrorCode.EMPTY_CART, "Cart is empty. Add items before checking out.");
    }
}
