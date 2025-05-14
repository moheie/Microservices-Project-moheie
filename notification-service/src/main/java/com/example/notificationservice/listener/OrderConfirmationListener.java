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
    
    @RabbitListener(queues = "order-confirmation")
    public void handleOrderConfirmation(String message) {
        System.out.println("Order Confirmation: " + message);
        
        // Parse the message - expecting format: "orderId:status:userId"
        String[] parts = message.split(":");
        if (parts.length >= 2) {
            String orderId = parts[0];
            String status = parts[1];
            Long userId = parts.length > 2 ? Long.parseLong(parts[2]) : null;
            
            // Send notification to customer if we have a user ID
            if (userId != null) {
                Notification customerNotification = notificationService.createOrderStatusNotification(
                        orderId, status, userId);
                notificationService.sendToUser(customerNotification, userId);
            }
            
            // Send notification to seller
            Notification sellerNotification = notificationService.createSellerOrderNotification(
                    orderId, status);
            notificationService.sendToSellers(sellerNotification);
        }
    }
}