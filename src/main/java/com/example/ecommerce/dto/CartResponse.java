package com.example.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;

public class CartResponse {

    private List<CartItemResponse> items;
    private int itemCount;
    private BigDecimal total;

    public CartResponse(List<CartItemResponse> items, int itemCount, BigDecimal total) {
        this.items = items;
        this.itemCount = itemCount;
        this.total = total;
    }

    public List<CartItemResponse> getItems() {
        return items;
    }

    public int getItemCount() {
        return itemCount;
    }

    public BigDecimal getTotal() {
        return total;
    }
}
