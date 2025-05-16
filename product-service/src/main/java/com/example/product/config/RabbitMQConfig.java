package com.example.product.config;

import com.example.product.model.Dish;
import com.example.product.service.DishService;
import com.rabbitmq.client.DeliverCallback;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Singleton
@Startup
public class RabbitMQConfig {
    private Connection connection;
    private Channel channel;

    @PersistenceContext(unitName = "product-service")
    private EntityManager entityManager;

    @Inject
    private DishService dishService;

    public static final String STOCK_CONFIRMATION_QUEUE = "stock-confirmation";
    private static final String ORDER_STOCK_CHECK_QUEUE = "order-stock-check";

    public static final String SELLER_STOCK_CHECK_QUEUE = "seller-stock-check";
    public static final String ADMIN_LOG_EXCHANGE = "admin-log";

    @PostConstruct
    public void init() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(5672);
            factory.setUsername("guest");
            factory.setPassword("guest");

            connection = factory.newConnection();
            channel = connection.createChannel();

            // Ensure exchanges exist
            channel.exchangeDeclare(ADMIN_LOG_EXCHANGE, "topic", true);            
            channel.queueDeclare(ORDER_STOCK_CHECK_QUEUE, false, false, false, null);
            channel.queueDeclare(STOCK_CONFIRMATION_QUEUE, false, false, false, null);
            channel.queueDeclare(SELLER_STOCK_CHECK_QUEUE, false, false, false, null);
            channel.queueDeclare("admin-log", true, false, false, null);
            
            // Bind the admin logs queue to the exchange
            channel.queueBind("admin-log", ADMIN_LOG_EXCHANGE, "*_Error");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                processStockCheckRequest(message);
            };

            channel.basicConsume(ORDER_STOCK_CHECK_QUEUE, true, deliverCallback, consumerTag -> {});
            

        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to initialize RabbitMQ connection", e);
        }
    }

    public Channel getChannel() {
        return channel;
    }

    private void processStockCheckRequest(String message) {
        try {
            String[] parts = message.split(":");
            if (parts.length != 2) return;

            Long orderId = Long.valueOf(parts[0]);
            String[] productIdStrings = parts[1].split(",");
            List<Long> productIds = Arrays.stream(productIdStrings)
                    .map(Long::valueOf)
                    .collect(Collectors.toList());

            Map<Long, Long> productCounts = productIds.stream()
                    .collect(Collectors.groupingBy(id -> id, Collectors.counting()));

            System.out.println("\u001B[35m Processing order: \u001B[0m " + orderId +
                    " with products: " + productCounts);

            boolean allInStock = checkStock(productCounts);
            double totalPrice = calculateTotalPrice(productCounts);

            String response = orderId + ":" + allInStock + ":" + totalPrice;
            channel.basicPublish("", STOCK_CONFIRMATION_QUEUE, null,
                    response.getBytes(StandardCharsets.UTF_8));

            if (allInStock) {
                dishService.decreaseStock(productCounts);
                System.out.println("\u001B[35m Stock decreased for order ID: \u001B[0m " + orderId);
            } else {
                System.out.println("\u001B[31m Insufficient stock for order ID: \u001B[0m " + orderId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            
            try {
                // Send error to admin log exchange
                if (channel != null) {
                    String errorMessage = "Product_Error:" + e.getMessage();
                    channel.basicPublish(ADMIN_LOG_EXCHANGE, "Product_Error", null, 
                        errorMessage.getBytes(StandardCharsets.UTF_8));
                    System.out.println("\u001B[31m Sent error to admin log: " + errorMessage + "\u001B[0m");
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private boolean checkStock(Map<Long, Long> productCounts) {
        for (Map.Entry<Long, Long> entry : productCounts.entrySet()) {
            Long productId = entry.getKey();
            Long quantity = entry.getValue();

            System.out.println("\u001B[35m Checking stock for product ID: \u001B[0m " +
                    productId + " (quantity needed: " + quantity + ")");

            Dish dish = entityManager.find(Dish.class, productId);
            if (dish == null || dish.getStockCount() < quantity) {
                System.out.println("\u001B[31m Product " + productId +
                        " insufficient stock: available=" +
                        (dish != null ? dish.getStockCount() : 0) +
                        ", needed=" + quantity + "\u001B[0m");
                return false;
            }
        }
        return true;
    }


    private double calculateTotalPrice(Map<Long, Long> productCounts) {
        double totalPrice = 0.0;
        for (Map.Entry<Long, Long> entry : productCounts.entrySet()) {
            Long productId = entry.getKey();
            Long quantity = entry.getValue();

            Dish dish = entityManager.find(Dish.class, productId);
            if (dish != null) {
                double itemTotal = dish.getPrice() * quantity;
                totalPrice += itemTotal;
                System.out.println("\u001B[36m Product " + productId +
                        ": $" + dish.getPrice() + " x " + quantity +
                        " = $" + itemTotal + "\u001B[0m");
            }
        }
        System.out.println("\u001B[36m Total price: $" + totalPrice + "\u001B[0m");
        return totalPrice;
    }



    @PreDestroy
    public void cleanup() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
