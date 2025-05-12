package com.example.product.messaging;

import com.example.product.model.Dish;
import com.example.product.service.DishService;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import com.rabbitmq.client.*;

@Singleton
@Startup
public class StockCheckListener {
    private static final String ORDER_STOCK_CHECK_QUEUE = "order-stock-check";
    public static final String ORDER_CONFIRMATION_QUEUE = "stock-confirmation";

    @PersistenceContext(unitName = "product-service")
    private EntityManager entityManager;

    private Connection connection;
    private Channel channel;

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

            channel.queueDeclare(ORDER_STOCK_CHECK_QUEUE, false, false, false, null);
            channel.queueDeclare(ORDER_CONFIRMATION_QUEUE, false, false, false, null);

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                processStockCheckRequest(message);
            };

            channel.basicConsume(ORDER_STOCK_CHECK_QUEUE, true, deliverCallback, consumerTag -> {});
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            channel.basicPublish("", ORDER_CONFIRMATION_QUEUE, null,
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
}