package com.example.orderservice.service;

import com.example.orderservice.config.RabbitMQConfig;
import com.example.orderservice.dto.CompanyOrderDTO;
import com.example.orderservice.dto.StockCheckRequest;
import com.example.orderservice.dto.StockConfirmationResponse;
import com.example.orderservice.messaging.NotificationSender;
import com.example.orderservice.model.Cart;
import com.example.orderservice.model.Order;
import com.example.orderservice.model.OrderDish;
import com.example.orderservice.model.OrderStatus;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.utils.Jwt;
import com.fasterxml.jackson.databind.ObjectMapper;
// Remove this import since we'll use the one from RabbitMQConfig
// import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.DeliverCallback;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            System.out.println("\u001B[33m Setting up consumer for queue: " + RabbitMQConfig.STOCK_CONFIRMATION_QUEUE + " \u001B[0m");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                System.out.println("\u001B[34m Received stock confirmation message: " + message + " \u001B[0m");

                try {
                    // Parse the JSON message to extract confirmation details
                    StockConfirmationResponse response = rabbitMQConfig.getObjectMapper()
                            .readValue(message, StockConfirmationResponse.class);

                    System.out.println("\u001B[34m Parsed stock confirmation - Order ID: " +
                            response.getOrderId() + ", In stock: " + response.isInStock() +
                            ", Total price: $" + response.getTotalPrice() + " \u001B[0m");

                    // Process the order based on stock confirmation
                    processOrder(response.getOrderId(), response.isInStock(), response.getTotalPrice());
                } catch (Exception e) {
                    System.err.println("\u001B[31m Failed to process stock confirmation: " + e.getMessage() + " \u001B[0m");
                    e.printStackTrace();
                }
            };

            rabbitMQConfig.getChannel().basicConsume(
                    RabbitMQConfig.STOCK_CONFIRMATION_QUEUE,
                    true,  // auto-acknowledge
                    deliverCallback,
                    consumerTag -> {
                        System.err.println("\u001B[31m Consumer cancelled: " + consumerTag + " \u001B[0m");
                    });

            System.out.println("\u001B[32m Stock confirmation consumer registered successfully \u001B[0m");
        } catch (IOException e) {
            System.err.println("\u001B[31m Failed to set up RabbitMQ consumer: " + e.getMessage() + " \u001B[0m");
            throw new RuntimeException("Failed to set up RabbitMQ consumer", e);
        }
    }

    public Order createOrderFromCart(String token) {
        try {
            Long userId = Jwt.getUserId(token);

            // Initialize cart if not already initialized
            cartService.initializeCart(token);
            Cart cart = cartService.getCurrentCart();

            // Check if cart is empty
            if (cart.getDishes() == null || cart.getDishes().isEmpty()) {
                throw new IllegalStateException("Cannot create an order from an empty cart");
            }

            // Create new order from cart
            Order order = new Order();
            order.setUserId(userId);
            order.setStatus(OrderStatus.PENDING);
            order.setCreatedAt(LocalDateTime.now());

            // Copy dishes from cart to order
            List<OrderDish> orderDishes = new ArrayList<>();
            for (OrderDish dish : cart.getDishes()) {
                OrderDish orderDish = new OrderDish(
                        dish.getDishId(),
                        dish.getName(),
                        dish.getCompanyName(),
                        dish.getPrice(),
                        dish.getQuantity()
                );
                orderDishes.add(orderDish);
            }
            order.setDishes(orderDishes);

            // Calculate cart total before proceeding
            double cartTotal = calculateCartTotal(cart);

            // Save order to database to get ID
            order = orderRepository.save(order);

            // Check minimum charge requirement before sending stock check
            if (cartTotal < MINIMUM_CHARGE) {
                // Set order status to CANCELED instead of throwing an exception
                order.setStatus(OrderStatus.CANCELED);
                order = orderRepository.save(order);

                // Notify user about cancellation due to minimum charge requirement
//                notificationSender.sendUserNotification(
//                        userId,
//                        "Order Canceled",
//                        "Your order #" + order.getId() + " was canceled because the total ($" +
//                                cartTotal + ") does not meet the minimum order requirement of $" + MINIMUM_CHARGE
//                );
//
//                // Log the cancellation
//                notificationSender.sendAdminLog("Order", "Warning",
//                        "Order #" + order.getId() + " was canceled due to not meeting minimum charge requirement"
//                );

                // Clear cart after order creation
                cartService.clearCart();
                cartService.persistCart();

                return order;
            }

            // Only check product stock if minimum charge is met
            checkProductStock(order);

            // Clear cart after order creation
            cartService.clearCart();
            cartService.persistCart();

            return order;
        } catch (Exception e) {
            System.err.println("\u001B[31m Error creating order from cart: " + e.getMessage() + " \u001B[0m");

            // Log the error to admin log queue
            try {
                //notificationSender.sendAdminLog("Order", "Error",
                        //"Failed to create order: " + e.getMessage());
            } catch (Exception logError) {
                System.err.println("\u001B[31m Failed to log order creation error: " + logError.getMessage() + " \u001B[0m");
            }

            throw new RuntimeException("Failed to create order: " + e.getMessage(), e);
        }
    }


    private double calculateCartTotal(Cart cart) {
        double total = 0.0;
        for (OrderDish dish : cart.getDishes()) {
            total += dish.getPrice() * dish.getQuantity();
        }
        return total;
    }
    private void checkProductStock(Order order) {
        try {
            // Create a map of product IDs to quantities
            Map<Long, Integer> productQuantities = new HashMap<>();

            // Populate the map from OrderDish objects
            for (OrderDish dish : order.getDishes()) {
                productQuantities.put(dish.getDishId(), dish.getQuantity());
            }

            // Create request object
            StockCheckRequest stockCheckRequest = new StockCheckRequest(order.getId(), productQuantities);

            System.out.println("\u001B[34m === PREPARING STOCK CHECK REQUEST === \u001B[0m");
            System.out.println("\u001B[34m Order ID: " + order.getId() + " \u001B[0m");
            System.out.println("\u001B[34m Products: " + productQuantities + " \u001B[0m");

            // Convert request to JSON using the ObjectMapper from RabbitMQConfig
            String jsonRequest = rabbitMQConfig.getObjectMapper().writeValueAsString(stockCheckRequest);

            // Send to RabbitMQ queue
            rabbitMQConfig.getChannel().basicPublish(
                    "",  // Default exchange
                    RabbitMQConfig.ORDER_STOCK_CHECK_QUEUE,  // Queue name
                    null,
                    jsonRequest.getBytes(StandardCharsets.UTF_8)
            );

            System.out.println("\u001B[34m === STOCK CHECK REQUEST SENT === \u001B[0m");

        } catch (Exception e) {
            System.err.println("\u001B[31m Error sending stock check request: " + e.getMessage() + " \u001B[0m");
            e.printStackTrace();

            // Log the error to admin log queue
            try {
                //notificationSender.sendAdminLog("Order", "Error",
                        //"Failed to check product stock for order " + order.getId() + ": " + e.getMessage());
            } catch (Exception logError) {
                System.err.println("Failed to send error log: " + logError.getMessage());
            }

            throw new RuntimeException("Failed to check product stock", e);
        }
    }

    private void processOrder(Long orderId, boolean inStock, double totalPrice) throws InterruptedException {
        Order order = orderRepository.findById(orderId);

        if (order == null) {
            System.err.println("\u001B[31m Order not found with ID: " + orderId + " \u001B[0m");
            return;
        }

        System.out.println("\u001B[34m Processing order " + orderId + " - In stock: " + inStock + " - Total price: $" + totalPrice + " \u001B[0m");

        if (inStock) {
            // If price is too low, cancel order
            if (totalPrice < MINIMUM_CHARGE) {
                order.setStatus(OrderStatus.CANCELED);
                orderRepository.save(order);
                System.out.println("\u001B[33m Order " + orderId + " canceled: Below minimum charge \u001B[0m");

                // Send notification to user about order cancellation
                // TODO : yousfi
                //notificationSender.sendOrderBelowMinimumNotification(order.getUserId(), MINIMUM_CHARGE);
            } else {
                // Update to being delivered
                order.setStatus(OrderStatus.BEING_DELIVERED);
                orderRepository.save(order);
                System.out.println("\u001B[32m Order " + orderId + " updated to BEING_DELIVERED \u001B[0m");

                // Send notification to user about successful order
                notificationSender.sendOrderConfirmation(order.getUserId(), "Your order is being processed", orderId);
            }
        } else {
            // Not enough stock, cancel order
            order.setStatus(OrderStatus.CANCELED);
            orderRepository.save(order);
            System.out.println("\u001B[33m Order " + orderId + " canceled: Insufficient stock \u001B[0m");

            // Send notification to user about stock issue
            // TODO : yousfi
            //notificationSender.sendInsufficientStockNotification(order.getUserId(), orderId);
        }
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId);
    }

    public List<CompanyOrderDTO> getOrdersByCompany(String token) {
        String companyName = Jwt.getCompany(token);

        if (companyName == null || companyName.isEmpty()) {
            throw new SecurityException("Only company users can access company orders");
        }

        return orderRepository.findByCompanyName(companyName);
    }

    public List<Order> getOrders(String token) {
        Long userId = Jwt.getUserId(token);
        return orderRepository.findByUserId(userId);
    }

}