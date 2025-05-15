package com.example.orderservice.repository;

import com.example.orderservice.model.Cart;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.NoResultException;

import java.io.Serializable;

@Stateless
public class CartRepository implements Serializable {
    @PersistenceContext
    private EntityManager entityManager;

    public Cart findByUserId(Long userId) {
        try {
            return entityManager.createQuery("SELECT c FROM Cart c WHERE c.userId = :userId", Cart.class)
                    .setParameter("userId", userId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Cart save(Cart cart) {
        if (cart.getId() == null) {
            entityManager.persist(cart);
        } else {
            cart = entityManager.merge(cart);
        }
        return cart;
    }

    public void delete(Cart cart) {
        entityManager.remove(entityManager.contains(cart) ? cart : entityManager.merge(cart));
    }

}