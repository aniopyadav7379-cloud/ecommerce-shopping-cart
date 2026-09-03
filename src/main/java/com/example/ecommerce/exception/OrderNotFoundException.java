package com.example.ecommerce.exception;

import org.springframework.http.HttpStatus;

public class OrderNotFoundException extends ApiException {
    public OrderNotFoundException(Long orderId) {
        super(HttpStatus.NOT_FOUND, ErrorCode.ORDER_NOT_FOUND, "Order not found with id: " + orderId);
    }
}
