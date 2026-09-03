package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class CartItemNotFoundException extends ApiException {
    public CartItemNotFoundException(Long productId) {
        super(HttpStatus.NOT_FOUND, ErrorCode.CART_ITEM_NOT_FOUND, "No cart item found for product id: " + productId);
    }
}
