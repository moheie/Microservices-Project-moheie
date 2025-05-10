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
    private String currentUserId;

    public void initializeCart(String token) {
        // Extract userId from JWT token
        String userId = Jwt.getUserId(token);
        this.currentUserId = userId;

        // Find or create user's cart
        this.currentCart = cartRepository.findByUserId(userId);
        if (this.currentCart == null) {
            this.currentCart = new Cart();
            this.currentCart.setUserId(userId);
            this.currentCart = cartRepository.save(this.currentCart);
        }
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
        this.currentCart = cartRepository.save(this.currentCart);
    }

    public void clearCart() {
        if (this.currentCart != null) {
            cartRepository.delete(this.currentCart);
            this.currentCart = null;
        }
    }

    public String getCurrentUserId() {
        return currentUserId;
    }

    @PreDestroy
    public void cleanup() {
        // Perform any cleanup if needed when bean is destroyed
        currentCart = null;
        currentUserId = null;
    }
}