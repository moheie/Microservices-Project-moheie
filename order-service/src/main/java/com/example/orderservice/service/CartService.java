package com.example.orderservice.service;

import com.example.orderservice.model.Cart;
import com.example.orderservice.repository.CartRepository;
import com.example.orderservice.utils.Jwt;
import jakarta.ejb.Stateful;
import jakarta.inject.Inject;
import jakarta.annotation.PreDestroy;

@Stateful
public class CartService {
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

        // Create a new in-memory cart
        this.currentCart = new Cart();
        this.currentCart.setUserId(userId);
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

        for (int i = 0; i < quantity; i++) {
            this.currentCart.addProductId(productId);
        }
        this.isDirty = true;
    }

    public void removeProductFromCart(Long productId, int quantity) {
        if (this.currentCart == null) {
            throw new IllegalStateException("Cart not initialized. Call initializeCart first.");
        }

        for (int i = 0; i < quantity; i++) {
            this.currentCart.removeProductId(productId);
        }
        this.isDirty = true;
    }

    public void clearCart() {
        if (this.currentCart != null) {
            this.currentCart.getProductIds().clear();
            this.isDirty = true;
        }
    }

    public Long getCurrentUserId() {
        return currentUserId;
    }

    /**
     * Only call this method when you want to persist the cart to database
     * (e.g., when checking out)
     */
    public Cart persistCart() {
        if (this.currentCart != null && this.isDirty) {
            // Delete any existing cart for this user
            Cart existingCart = cartRepository.findByUserId(currentUserId);
            if (existingCart != null) {
                cartRepository.delete(existingCart);
            }

            // Save the current cart
            this.currentCart = cartRepository.save(this.currentCart);
            this.isDirty = false;
        }
        return this.currentCart;
    }

    @PreDestroy
    public void cleanup() {
        // Don't persist cart when bean is destroyed
        currentCart = null;
        currentUserId = null;
        isDirty = false;
    }
}