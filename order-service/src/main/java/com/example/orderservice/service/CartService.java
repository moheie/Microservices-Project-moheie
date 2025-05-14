package com.example.orderservice.service;

import com.example.orderservice.model.Cart;
import com.example.orderservice.model.OrderDish;
import com.example.orderservice.repository.CartRepository;
import com.example.orderservice.utils.Jwt;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class CartService implements Serializable {
    private static final long serialVersionUID = 1L;

    @Inject
    private CartRepository cartRepository;

    private Cart currentCart;
    private Long currentUserId;
    private boolean isDirty = false;

    public void initializeCart(String token) {
        // Extract userId from JWT token
        Long userId = Jwt.getUserId(token);

        // If the user is the same, keep using the existing cart
        if (this.currentUserId != null && this.currentUserId.equals(userId) && this.currentCart != null) {
            return;
        }

        this.currentUserId = userId;

        // Try to load existing cart from database
        Cart existingCart = cartRepository.findByUserId(userId);
        if (existingCart != null) {
            this.currentCart = existingCart;
        } else {
            // Create a new in-memory cart
            this.currentCart = new Cart();
            this.currentCart.setUserId(userId);
            this.currentCart.setUserName(Jwt.getUsername(token));
        }
        this.isDirty = false;
    }

    public Cart getCurrentCart() {
        if (this.currentCart == null) {
            throw new IllegalStateException("Cart not initialized. Call initializeCart first.");
        }
        return this.currentCart;
    }

    public void addProductToCart(Long productId) {
        if (this.currentCart == null) {
            throw new IllegalStateException("Cart not initialized. Call initializeCart first.");
        }

        this.currentCart.addProductId(productId);
        this.isDirty = true;
    }

    public void addProductsToCart(Long productId, int quantity, String dishName, double dishPrice, String companyName) {
        if (this.currentCart == null) {
            throw new IllegalStateException("Cart not initialized. Call initializeCart first.");
        }

        // Create the OrderDish object once
        OrderDish dish = new OrderDish(productId, dishName, companyName, dishPrice);

        // Add the specified quantity
        for (int i = 0; i < quantity; i++) {
            // Add to productIds for backward compatibility
            this.currentCart.addProductId(productId);

            // Add a new instance of the dish for each quantity
            // This ensures that removing one dish doesn't remove all of them
            this.currentCart.addDish(new OrderDish(productId, dishName, companyName, dishPrice));
        }

        this.isDirty = true;
    }

    public void removeProductFromCart(Long productId, int quantity) {
        if (this.currentCart == null) {
            throw new IllegalStateException("Cart not initialized. Call initializeCart first.");
        }

        for (int i = 0; i < quantity; i++) {
            this.currentCart.removeProductId(productId);
            this.currentCart.removeDish(productId);
        }
        this.isDirty = true;
    }

    public void clearCart() {
        if (this.currentCart != null) {
            this.currentCart.getProductIds().clear();
            this.currentCart.getDishes().clear();
            this.isDirty = true;
        }
    }

    public Long getCurrentUserId() {
        return currentUserId;
    }

    /**
     * Persists the cart to database
     */
    public Cart persistCart() {
        if (this.currentCart != null && this.isDirty) {
            this.currentCart = cartRepository.save(this.currentCart);
            this.isDirty = false;
        }
        return this.currentCart;
    }

    @PreDestroy
    public void cleanup() {
        // Optionally persist cart when session ends
        if (isDirty && currentCart != null) {
            try {
                persistCart();
            } catch (Exception e) {
                // Log the exception
                System.err.println("Failed to persist cart during cleanup: " + e.getMessage());
            }
        }
    }
}