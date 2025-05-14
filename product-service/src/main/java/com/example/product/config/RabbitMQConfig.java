package com.example.product.config;

import com.example.product.model.Dish;
import com.rabbitmq.client.DeliverCallback;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

@Singleton
@Startup
public class RabbitMQConfig {
    private Connection connection;
    private Channel channel;

    @PersistenceContext(unitName = "product-service")
    private EntityManager entityManager;

    public static final String STOCK_CONFIRMATION_QUEUE = "stock-confirmation";
    private static final String ORDER_STOCK_CHECK_QUEUE = "order-stock-check";

    public static final String STOCK_CHECK_QUEUE = "stock-check";
    public static final String LOG_EXCHANGE = "log";
    public static final String PAYMENTS_EXCHANGE = "payments-exchange";

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
            channel.exchangeDeclare(LOG_EXCHANGE, "topic", true);
            channel.exchangeDeclare(PAYMENTS_EXCHANGE, "direct", true);

            channel.queueDeclare(ORDER_STOCK_CHECK_QUEUE, true, false, false, null);
            channel.queueDeclare(STOCK_CONFIRMATION_QUEUE, true, false, false, null);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                processStockCheckRequest(message);
            };

            channel.basicConsume(ORDER_STOCK_CHECK_QUEUE, true, deliverCallback, consumerTag -> {});
            
            // Declare queues
            channel.queueDeclare(STOCK_CHECK_QUEUE, true, false, false, null);
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

            boolean allInStock = checkStock(productIds);

            double totalPrice = calculateTotalPrice(productIds);
            String response = orderId + ":" + allInStock + ":" + totalPrice;
            channel.basicPublish("", STOCK_CONFIRMATION_QUEUE, null,
                    response.getBytes(StandardCharsets.UTF_8));

            // If in stock, decrease stock count
            if (allInStock) {
                decreaseStock(productIds);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean checkStock(List<Long> productIds) {
        for (Long productId : productIds) {
            Dish dish = entityManager.find(Dish.class, productId);
            if (dish == null || dish.getStockCount() <= 0) {
                return false;
            }
        }
        return true;
    }

    private void decreaseStock(List<Long> productIds) {
        for (Long productId : productIds) {
            Dish dish = entityManager.find(Dish.class, productId);
            if (dish != null) {
                dish.setStockCount(dish.getStockCount() - 1);
                entityManager.merge(dish);
            }
        }
    }
    private double calculateTotalPrice(List<Long> productIds) {
        double totalPrice = 0.0;
        for (Long productId : productIds) {
            Dish dish = entityManager.find(Dish.class, productId);
            if (dish != null) {
                totalPrice += dish.getPrice();
            }
        }
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
