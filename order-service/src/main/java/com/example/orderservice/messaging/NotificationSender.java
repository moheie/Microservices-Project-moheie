package com.example.orderservice.messaging;

import com.example.orderservice.config.RabbitMQConfig;
import com.rabbitmq.client.Channel;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Service for sending notifications via RabbitMQ
 */
@Stateless
public class NotificationSender {

    @Inject
    private RabbitMQConfig rabbitMQConfig;
    
    /**
     * Send an order confirmation notification
     * 
     * @param orderId The order ID
     * @param status The order status (e.g., "confirmed", "processing", "delivered")
     * @param userId The user ID of the customer
     */
    public void sendOrderConfirmation(Long orderId, String status, Long userId) {
        try {
            String message = orderId + ":" + status + ":" + userId;
            Channel channel = rabbitMQConfig.getChannel();
            
            channel.basicPublish(
                "",  // Default exchange
                "order-confirmation",  // Queue name
                null,
                message.getBytes(StandardCharsets.UTF_8)
            );
            
            System.out.println("Sent order confirmation: " + message);
        } catch (IOException e) {
            System.err.println("Failed to send order confirmation: " + e.getMessage());
        }
    }
    
    /**
     * Send a payment failed notification
     * 
     * @param orderId The order ID
     * @param reason The reason for payment failure
     */
    public void sendPaymentFailure(Long orderId, String reason) {
        try {
            String message = orderId + ":" + reason;
            Channel channel = rabbitMQConfig.getChannel();
            
            channel.basicPublish(
                "payments-exchange",  // Exchange
                "PaymentFailed",  // Routing key
                null,
                message.getBytes(StandardCharsets.UTF_8)
            );
            
            System.out.println("Sent payment failure notification: " + message);
        } catch (IOException e) {
            System.err.println("Failed to send payment failure notification: " + e.getMessage());
        }
    }
    
    /**
     * Send a log message
     * 
     * @param service The service name
     * @param severity The severity level (Info, Warning, Error)
     * @param message The log message
     */
    public void sendLogMessage(String service, String severity, String message) {
        try {
            String routingKey = service + "_" + severity;
            String logMessage = routingKey + ":" + message;
            Channel channel = rabbitMQConfig.getChannel();
            
            channel.basicPublish(
                "log",  // Exchange
                routingKey,  // Routing key
                null,
                logMessage.getBytes(StandardCharsets.UTF_8)
            );
            
            System.out.println("Sent log message: " + logMessage);
        } catch (IOException e) {
            System.err.println("Failed to send log message: " + e.getMessage());
        }
    }
    
    /**
     * Send a stock check notification
     * 
     * @param productId The product ID
     * @param productName The product name
     * @param quantity The current stock quantity
     */
    public void sendStockCheck(String productId, String productName, int quantity) {
        try {
            String message = productId + ":" + productName + ":" + quantity;
            Channel channel = rabbitMQConfig.getChannel();
            
            channel.basicPublish(
                "",  // Default exchange
                "stock-check",  // Queue name
                null,
                message.getBytes(StandardCharsets.UTF_8)
            );
            
            System.out.println("Sent stock check notification: " + message);
        } catch (IOException e) {
            System.err.println("Failed to send stock check notification: " + e.getMessage());
        }
    }
}
