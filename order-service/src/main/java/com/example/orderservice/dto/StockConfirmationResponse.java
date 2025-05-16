package com.example.orderservice.dto;

import java.io.Serializable;

public class StockConfirmationResponse implements Serializable {
    private Long orderId;
    private boolean inStock;
    private double totalPrice;

    // Default constructor for serialization
    public StockConfirmationResponse() {}

    public StockConfirmationResponse(Long orderId, boolean inStock, double totalPrice) {
        this.orderId = orderId;
        this.inStock = inStock;
        this.totalPrice = totalPrice;
    }

    // Getters and setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }

    public double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(double totalPrice) { this.totalPrice = totalPrice; }
}
