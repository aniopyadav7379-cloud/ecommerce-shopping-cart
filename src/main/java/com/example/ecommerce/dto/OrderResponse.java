package com.example.ecommerce.dto;

import com.example.ecommerce.model.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class OrderResponse {

    private Long id;
    private String customerName;
    private String email;
    private String shippingAddress;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    public static OrderResponse from(Order order) {
        OrderResponse dto = new OrderResponse();
        dto.id = order.getId();
        dto.customerName = order.getCustomerName();
        dto.email = order.getEmail();
        dto.shippingAddress = order.getShippingAddress();
        dto.totalAmount = order.getTotalAmount();
        dto.status = order.getStatus().name();
        dto.createdAt = order.getCreatedAt();
        dto.items = order.getItems().stream().map(OrderItemResponse::from).collect(Collectors.toList());
        return dto;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getEmail() {
        return email;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public List<OrderItemResponse> getItems() {
        return items;
    }
}
