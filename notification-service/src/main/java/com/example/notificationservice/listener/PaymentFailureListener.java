package com.example.notificationservice.listener;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentFailureListener {
    
    private final NotificationService notificationService;
    
    @Autowired
    public PaymentFailureListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @RabbitListener(queues = "payment-failed")
    public void handlePaymentFailure(String message) {
        System.out.println("Payment Failure Notification: " + message);
        
        // Parse the message - expecting format: "orderId:reason"
        String[] parts = message.split(":", 2);
        String orderId = parts[0];
        String reason = parts.length > 1 ? parts[1] : "Unknown reason";
        
        // Create and send notification to admins
        Notification notification = notificationService.createPaymentFailedNotification(orderId, reason);
        notificationService.sendToAdmins(notification);
    }
}