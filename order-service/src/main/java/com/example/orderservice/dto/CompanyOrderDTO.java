package com.example.orderservice.dto;

import com.example.orderservice.model.OrderDish;
import com.example.orderservice.model.OrderStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CompanyOrderDTO {
    private Long id;
    private Long userId;
    private List<OrderDish> companyDishes = new ArrayList<>();
    private OrderStatus status;
    private LocalDateTime createdAt;
    private String companyName;

    public CompanyOrderDTO() {
    }

    public CompanyOrderDTO(Long id, Long userId, OrderStatus status, LocalDateTime createdAt, String companyName) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.createdAt = createdAt;
        this.companyName = companyName;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public List<OrderDish> getCompanyDishes() { return companyDishes; }
    public void setCompanyDishes(List<OrderDish> companyDishes) { this.companyDishes = companyDishes; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
}