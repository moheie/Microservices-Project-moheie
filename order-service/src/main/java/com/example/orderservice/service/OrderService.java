package com.example.orderservice.service;

import com.example.orderservice.config.RabbitMQConfig;
import com.example.orderservice.model.Cart;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.utils.Jwt;
import com.rabbitmq.client.DeliverCallback;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

@Stateless
public class OrderService {
    @Inject
    private OrderRepository orderRepository;

    @EJB
    private CartService cartService;

    @Inject
    private RabbitMQConfig rabbitMQConfig;

    @PostConstruct
    public void setupConsumer() {
        try {
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                String[] parts = message.split(":");
                if (parts.length == 2) {
                    Long orderId = Long.valueOf(parts[0]);
                    boolean inStock = Boolean.parseBoolean(parts[1]);
                    updateOrderStatus(orderId, inStock);
                }
            };

            rabbitMQConfig.getChannel().basicConsume(
                    RabbitMQConfig.STOCK_CONFIRMATION_QUEUE,
                    true,
                    deliverCallback,
                    consumerTag -> {}
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to set up RabbitMQ consumer", e);
        }
    }

    public Order createOrderFromCart(String token) {
        Long userId = Jwt.getUserId(token);
        cartService.initializeCart(token);
        Cart cart = cartService.getCurrentCart();

        if (cart.getProductIds().isEmpty()) {
            throw new IllegalStateException("Cannot create order from empty cart");
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setProductIds(cart.getProductIds());
        order.setStatus(OrderStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());

        order = orderRepository.save(order);

        // Send message to check stock
        checkProductStock(order);

        // Clear the cart after creating the order
        cartService.clearCart();

        return order;
    }

    private void checkProductStock(Order order) {
        try {
            String message = order.getId() + ":" + String.join(",",
                    order.getProductIds().stream().map(Object::toString).toArray(String[]::new));

            rabbitMQConfig.getChannel().basicPublish("",
                    RabbitMQConfig.ORDER_STOCK_CHECK_QUEUE,
                    null,
                    message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to send stock check message", e);
        }
    }

    public void updateOrderStatus(Long orderId, boolean productsInStock) {
        Order order = orderRepository.findById(orderId);
        if (order != null) {
            order.setStatus(productsInStock ? OrderStatus.BEING_DELIVERED : OrderStatus.CANCELED);
            orderRepository.save(order);
        }
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId);
    }
}