package com.example.orderservice.repository;

import com.example.orderservice.model.Order;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class OrderRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public Order findById(Long id) {
        return entityManager.find(Order.class, id);
    }

    public Order save(Order order) {
        if (order.getId() == null) {
            entityManager.persist(order);
        } else {
            order = entityManager.merge(order);
        }
        return order;
    }
}