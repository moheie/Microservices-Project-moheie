package com.example.product.dto;

import java.io.Serializable;
import java.util.Map;

public class StockCheckRequest implements Serializable {
    private Long orderId;
    private Map<Long, Integer> productQuantities; // productId -> quantity mapping

    // Default constructor for deserialization
    public StockCheckRequest() {}

    public StockCheckRequest(Long orderId, Map<Long, Integer> productQuantities) {
        this.orderId = orderId;
        this.productQuantities = productQuantities;
    }

    // Getters and setters
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }

    public Map<Long, Integer> getProductQuantities() { return productQuantities; }
    public void setProductQuantities(Map<Long, Integer> productQuantities) { this.productQuantities = productQuantities; }

    @Override
    public String toString() {
        return "StockCheckRequest{orderId=" + orderId + ", productQuantities=" + productQuantities + "}";
    }
}