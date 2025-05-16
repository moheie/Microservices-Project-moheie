package com.example.orderservice.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders") // "order" is a reserved keyword in SQL
@NamedQueries({
        @NamedQuery(name = "Order.findByUserId",
                query = "SELECT o FROM Order o WHERE o.userId = :userId"),
        @NamedQuery(name = "Order.findByStatus",
                query = "SELECT o FROM Order o WHERE o.status = :status"),
        @NamedQuery(name = "Order.findById",
                query = "SELECT o FROM Order o WHERE o.id = :id"),
        @NamedQuery(name = "Order.findByCompanyName",
                query = "SELECT o FROM Order o JOIN o.dishes d WHERE d.companyName = :companyName")
})
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private List<OrderDish> dishes = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    private LocalDateTime createdAt;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public List<OrderDish> getDishes() { return dishes; }
    public void setDishes(List<OrderDish> dishes) { this.dishes = dishes; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}