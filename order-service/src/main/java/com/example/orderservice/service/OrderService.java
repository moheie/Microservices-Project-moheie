package com.example.orderservice.service;

import com.example.orderservice.config.RabbitMQConfig;
import com.example.orderservice.dto.CompanyOrderDTO;
import com.example.orderservice.messaging.NotificationSender;
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
import java.util.List;

@Stateless
public class OrderService {
    @Inject
    private OrderRepository orderRepository;

    @Inject
    private CartService cartService;

    @Inject
    private RabbitMQConfig rabbitMQConfig;

    @Inject
    private NotificationSender notificationSender;

    private static final double MINIMUM_CHARGE = 50.0;

    @PostConstruct
    public void setupConsumer() {
        try {
            System.out.println("Setting up consumer for queue: " + RabbitMQConfig.STOCK_CONFIRMATION_QUEUE);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("Received message: " + message);

                String[] parts = message.split(":");
                if (parts.length == 3) {
                    try {
                        Long orderId = Long.valueOf(parts[0]);
                        boolean inStock = Boolean.parseBoolean(parts[1]);
                        double totalPrice = Double.parseDouble(parts[2]);
                        System.out.println("Processing order: " + orderId + ", inStock: " + inStock + ", totalPrice: " + totalPrice);
                        processOrder(orderId, inStock, totalPrice);
                    } catch (NumberFormatException e) {
                        System.err.println("Error parsing message: " + message + " - " + e.getMessage());
                    }
                } else {
                    System.err.println("Invalid message format: " + message);
                }
            };

            rabbitMQConfig.getChannel().basicConsume(
                    RabbitMQConfig.STOCK_CONFIRMATION_QUEUE,
                    true,
                    deliverCallback,
                    consumerTag -> {}
            );
        } catch (IOException e) {
            System.err.println("Failed to set up RabbitMQ consumer: " + e.getMessage());
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

        // Clear the cart after creating the order but don't persist changes
        cartService.clearCart();
        // We don't call persistCart() since we want the cart to stay in memory only

        return order;
    }


    private void checkProductStock(Order order) {
        try {
            //double totalPrice = calculateTotalPrice(order);
            String message = order.getId() + ":" + String.join(",",
                    order.getProductIds().stream().map(Object::toString).toArray(String[]::new)) /*+ ":" + totalPrice*/;

            rabbitMQConfig.getChannel().basicPublish("",
                    RabbitMQConfig.ORDER_STOCK_CHECK_QUEUE,
                    null,
                    message.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Failed to send stock check message", e);
        }
    }


    private void processOrder(Long orderId, boolean inStock, double totalPrice) {
        if (orderId == null) {
            System.err.println("Cannot process order: orderId is null");
            notificationSender.sendLogMessage("Order", "Error", "Cannot process order: orderId is null");
            return;
        }

        Order order = orderRepository.findById(orderId);
        if (order == null) {
            System.err.println("Order not found with id: " + orderId);
            notificationSender.sendLogMessage("Order", "Error", "Order not found with id: " + orderId);
            return;
        }
        
        if (inStock && totalPrice >= MINIMUM_CHARGE) {
            order.setStatus(OrderStatus.BEING_DELIVERED);
            // Proceed with payment (mock implementation)
            sendOrderConfirmation(order, true);
            
            // Send notifications
            notificationSender.sendOrderConfirmation(order.getId(), "confirmed", order.getUserId());
        } else {
            order.setStatus(OrderStatus.CANCELED);
            // Rollback actions (mock implementation)
            sendOrderConfirmation(order, false);
            
            // Send notifications
            if (!inStock) {
                notificationSender.sendLogMessage("Order", "Warning", 
                    "Order " + orderId + " canceled: Products not in stock");
                notificationSender.sendOrderConfirmation(order.getId(), "canceled - out of stock", order.getUserId());
            } else {
                notificationSender.sendLogMessage("Order", "Warning", 
                    "Order " + orderId + " canceled: Minimum charge not met");
                notificationSender.sendPaymentFailure(order.getId(), "Minimum charge of $" + MINIMUM_CHARGE + " not met");
                notificationSender.sendOrderConfirmation(order.getId(), "canceled - minimum charge not met", order.getUserId());
            }
        }

        orderRepository.save(order);
    }

//    public void updateOrderStatus(Long orderId, boolean productsInStock) {
//        Order order = orderRepository.findById(orderId);
//        if (order != null) {
//            order.setStatus(productsInStock ? OrderStatus.BEING_DELIVERED : OrderStatus.CANCELED);
//            orderRepository.save(order);
//        }
//    }

    private void sendOrderConfirmation(Order order, boolean success) {
        try {
            String message = "Order " + order.getId() + " " + (success ? "confirmed" : "canceled");
            rabbitMQConfig.getChannel().basicPublish("", RabbitMQConfig.STOCK_CONFIRMATION_QUEUE, null, message.getBytes(StandardCharsets.UTF_8));
            
            // Log the event
            if (!success) {
                notificationSender.sendLogMessage("Order", "Info", message);
            }
        } catch (IOException e) {
            notificationSender.sendLogMessage("Order", "Error", "Failed to send order confirmation: " + e.getMessage());
            throw new RuntimeException("Failed to send order confirmation", e);
        }
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<CompanyOrderDTO> getOrdersByCompany(String token) {
        String companyName = Jwt.getCompany(token);

        if (companyName == null || companyName.isEmpty()) {
            throw new SecurityException("No company associated with this token");
        }

        return orderRepository.findByCompanyName(companyName);
    }

    public List<Order> getOrders(String token) {
        Long userId = Jwt.getUserId(token);
        return orderRepository.findByUserId(userId);
    }
}