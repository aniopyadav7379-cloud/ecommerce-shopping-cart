package com.example.ecommerce.dto;

import com.example.ecommerce.model.CartItem;

import java.math.BigDecimal;

public class CartItemResponse {

    private Long productId;
    private String productName;
    private String imageUrl;
    private BigDecimal unitPrice;
    private Integer quantity;
    private Integer availableStock;
    private BigDecimal subtotal;

    public static CartItemResponse from(CartItem item) {
        CartItemResponse dto = new CartItemResponse();
        dto.productId = item.getProduct().getId();
        dto.productName = item.getProduct().getName();
        dto.imageUrl = item.getProduct().getImageUrl();
        dto.unitPrice = item.getProduct().getPrice();
        dto.quantity = item.getQuantity();
        dto.availableStock = item.getProduct().getStock();
        dto.subtotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return dto;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }
}
