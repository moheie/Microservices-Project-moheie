//package com.example.orderservice.service;
//
//import com.example.orderservice.config.RabbitMQConfig;
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.DeliverCallback;
//import jakarta.annotation.PostConstruct;
//import jakarta.ejb.Singleton;
//import jakarta.inject.Inject;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//
//@Singleton
//public class StockService {
//    @Inject
//    private RabbitMQConfig rabbitMQConfig;
//
//    @PostConstruct
//    public void setupConsumer() {
//        try {
//            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
//                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
//                String[] parts = message.split(":");
//                if (parts.length == 3) {
//                    Long orderId = Long.valueOf(parts[0]);
//                    String[] productIds = parts[1].split(",");
//                    double totalPrice = Double.parseDouble(parts[2]);
//
//                    boolean inStock = checkStock(productIds);
//                    String responseMessage = orderId + ":" + inStock + ":" + totalPrice;
//
//                    rabbitMQConfig.getChannel().basicPublish(
//                            RabbitMQConfig.ORDER_STOCK_CHECK_QUEUE,
//                            "",
//                            null,
//                            responseMessage.getBytes(StandardCharsets.UTF_8)
//                    );
//                }
//            };
//
//            rabbitMQConfig.getChannel().basicConsume(
//                    RabbitMQConfig.ORDER_CONFIRMATION_QUEUE,
//                    true,
//                    deliverCallback,
//                    consumerTag -> {}
//            );
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to set up RabbitMQ consumer", e);
//        }
//    }
//
//    //TODO: Implement actual stock check logic
//    private boolean checkStock(String[] productIds) {
//        // Mock implementation: Replace with actual stock check logic
//        return true; // Assume all products are in stock
//    }
//}