package com.example.orderservice.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Entity
public class Cart implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "cart_id")
    private List<OrderDish> dishes = new ArrayList<>();

    public Cart() {
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }


    public List<OrderDish> getDishes() { return dishes; }
    public void setDishes(List<OrderDish> dishes) { this.dishes = dishes; }

    public void addDish(OrderDish dish) {
        this.dishes.add(dish);
    }

    public void removeDish(Long dishId) {
        this.dishes.removeIf(dish -> dish.getDishId().equals(dishId));
    }

    // For backward compatibility
    @ElementCollection(fetch = FetchType.EAGER)
    private List<Long> productIds = new ArrayList<>();

    public List<Long> getProductIds() { return productIds; }
    public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
    public void addProductId(Long productId) { this.productIds.add(productId); }
    public void removeProductId(Long productId) { this.productIds.remove(productId); }
}