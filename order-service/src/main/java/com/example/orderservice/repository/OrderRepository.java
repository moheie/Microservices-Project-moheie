package com.example.orderservice.repository;

import com.example.orderservice.dto.CompanyOrderDTO;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderDish;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public List<CompanyOrderDTO> findByCompanyName(String companyName) {
        // First, get all orders that have at least one dish from the company
        TypedQuery<Order> query = entityManager.createNamedQuery("Order.findByCompanyName", Order.class)
                .setParameter("companyName", companyName);

        List<Order> orders = query.getResultList();

        // Transform Order entities to CompanyOrderDTOs with only relevant dishes
        List<CompanyOrderDTO> dtos = new ArrayList<>();

        for (Order order : orders) {
            CompanyOrderDTO dto = new CompanyOrderDTO(
                    order.getId(),
                    order.getUserId(),
                    order.getStatus(),
                    order.getCreatedAt(),
                    companyName
            );

            // Filter dishes to include only those from this company
            List<OrderDish> companyDishes = order.getDishes().stream()
                    .filter(dish -> companyName.equals(dish.getCompanyName()))
                    .collect(Collectors.toList());

            dto.setCompanyDishes(companyDishes);
            dtos.add(dto);
        }

        return dtos;
    }
    
    public List<Order> findByUserId(Long userId) {
        TypedQuery<Order> query = entityManager.createNamedQuery("Order.findByUserId", Order.class)
                .setParameter("userId", userId);
        return query.getResultList();
    }
}