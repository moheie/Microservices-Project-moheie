package com.example.orderservice.repository;

import com.example.orderservice.model.OrderItem;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@Stateless
public class OrderItemRepository {
    @PersistenceContext(unitName = "order-service")
    private EntityManager entityManager;
    
    public void save(OrderItem orderItem) {
        if (orderItem.getId() == null) {
            entityManager.persist(orderItem);
        } else {
            entityManager.merge(orderItem);
        }
    }
    
    public OrderItem findById(Long id) {
        return entityManager.find(OrderItem.class, id);
    }
    
    public List<OrderItem> findByOrderId(Long orderId) {
        return entityManager.createQuery(
                "SELECT i FROM OrderItem i WHERE i.orderId = :orderId", OrderItem.class)
                .setParameter("orderId", orderId)
                .getResultList();
    }
    
    public void delete(OrderItem orderItem) {
        entityManager.remove(entityManager.contains(orderItem) ? orderItem : entityManager.merge(orderItem));
    }
}
