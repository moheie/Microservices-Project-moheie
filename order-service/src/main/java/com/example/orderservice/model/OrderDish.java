package com.example.orderservice.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_dishes")
public class OrderDish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long dishId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String companyName;
    @Column(nullable = false)
    private double price;

    // Default constructor required by JPA
    public OrderDish() {}

    // Constructor with fields for direct creation
    public OrderDish(Long dishId, String name, String companyName, double price) {
        this.dishId = dishId;
        this.name = name;
        this.companyName = companyName;
        this.price = price;
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDishId() { return dishId; }
    public void setDishId(Long dishId) { this.dishId = dishId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}