package com.example.notificationservice.listener;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderConfirmationListener {
    
    private final NotificationService notificationService;
    
    @Autowired
    public OrderConfirmationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @RabbitListener(queues = "user-order-confirmation")
    public void handleOrderConfirmation(String message) {
        System.out.println("Order Confirmation: " + message);
        
        // Parse the message - expecting format: "orderId:status:userId" from NotificationSender.sendOrderConfirmation
        String[] parts = message.split(":");
        if (parts.length >= 3) {
            try {
                Long orderId = Long.parseLong(parts[0]);
                String status = parts[1];
                Long userId = Long.parseLong(parts[2]);
                
                // Send notification to customer
                Notification customerNotification = notificationService.createOrderStatusNotification(
                    orderId.toString(), status, userId);
                notificationService.sendToUser(customerNotification, userId);

                // If order status is "Failed", send a log message to admin
                if ("Failed".equalsIgnoreCase(status)) {
                    notificationService.sendLogMessage(
                        "Order",
                        "Error",
                        String.format("Order %s has failed for user %d", orderId, userId),
                        1L
                    );
                }

                Notification AdminNotification = notificationService.createOrderStatusNotification(
                    orderId.toString(), status, 1L); // Assuming 1L is the admin user ID
                notificationService.sendToUser(AdminNotification, 1L);
                
            } catch (NumberFormatException e) {
                System.err.println("Error parsing order confirmation message: " + e.getMessage());
            }
        } else {
            System.err.println("Invalid order confirmation message format: " + message);
        }
    }
}