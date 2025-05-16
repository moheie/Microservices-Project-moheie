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
    

    public void sendOrderConfirmation(Long orderId, String status, Long userId) {
        try {
            String message = orderId + ":" + status + ":" + userId;

            rabbitMQConfig.getChannel().basicPublish(
                    "",
                    RabbitMQConfig.USER_ORDER_CONFIRMATION_QUEUE,  // Use the correct queue
                    null,
                    message.getBytes(StandardCharsets.UTF_8)
            );
            
            System.out.println("Sent order confirmation: " + message);
        } catch (IOException e) {
            System.err.println("Failed to send order confirmation: " + e.getMessage());
        }
    }

    public void sendPaymentFailure(Long orderId, String reason) {
        try {
            String message = orderId + ":" + reason;

            rabbitMQConfig.getChannel().basicPublish(
                    RabbitMQConfig.PAYMENTS_EXCHANGE,
                    "PaymentFailed",
                    null,
                    message.getBytes(StandardCharsets.UTF_8)
            );
            
            System.out.println("Sent payment failure notification: " + message);
        } catch (IOException e) {
            System.err.println("Failed to send payment failure notification: " + e.getMessage());
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

//    public void sendOrderCancellation(Long orderId, String reason) {
//        try {
//            String message = orderId + ":" + reason;
//            Channel channel = rabbitMQConfig.getChannel();
//
//            channel.basicPublish(
//                "",  // Default exchange
//                "order-cancellation",  // Queue name
//                null,
//                message.getBytes(StandardCharsets.UTF_8)
//            );
//
//            System.out.println("Sent order cancellation: " + message);
//        } catch (IOException e) {
//            System.err.println("Failed to send order cancellation: " + e.getMessage());
//        }
//    }
    

}
