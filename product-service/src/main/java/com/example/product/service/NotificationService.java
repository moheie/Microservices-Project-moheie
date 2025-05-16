package com.example.product.service;

import com.example.product.config.RabbitMQConfig;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Stateless
public class NotificationService {
    
    @Inject
    private RabbitMQConfig rabbitMQConfig;
    

    public void sendStockNotification(Long productId, String productName, int quantity, String sellerCompanyName, Long sellerId) {
        try {
            String message = productName + ":" + quantity + ":" + sellerCompanyName + ":" + sellerId;
            rabbitMQConfig.getChannel().basicPublish(
                "",  // Default exchange
                RabbitMQConfig.SELLER_STOCK_CHECK_QUEUE,  // Queue name
                null,
                message.getBytes(StandardCharsets.UTF_8)
            );
            
            System.out.println("Sent stock notification for " + productName + " (quantity: " + quantity + ") to seller: " + sellerCompanyName);
        } catch (IOException e) {
            System.err.println("Failed to send stock notification: " + e.getMessage());
        }
    }
    

    public void sendLogMessage(String service, String severity, String message) {
        try {
            String routingKey = service + "_" + severity;
            String logMessage = routingKey + ":" + message;
            
            rabbitMQConfig.getChannel().basicPublish(
                RabbitMQConfig.ADMIN_LOG_EXCHANGE,  // Exchange
                routingKey,  // Routing key
                null,
                logMessage.getBytes(StandardCharsets.UTF_8)
            );
            
            System.out.println("Sent log message: " + logMessage);
        } catch (IOException e) {
            System.err.println("Failed to send log message: " + e.getMessage());
        }
    }
}
