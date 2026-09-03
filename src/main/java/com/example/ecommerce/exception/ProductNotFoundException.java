package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class ProductNotFoundException extends ApiException {
    public ProductNotFoundException(Long productId) {
        super(HttpStatus.NOT_FOUND, ErrorCode.PRODUCT_NOT_FOUND, "Product not found with id: " + productId);
    }
}
