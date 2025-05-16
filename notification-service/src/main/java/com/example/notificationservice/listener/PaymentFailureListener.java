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
        
        // Parse the message - expecting format: "orderId:reason" from NotificationSender.sendPaymentFailure
        String[] parts = message.split(":", 2);
        try {
            Long orderId = Long.parseLong(parts[0]);
            String reason = parts.length > 1 ? parts[1] : "Unknown reason";
            
            // Create notification for admins - in this case, we'll use a fixed admin ID (1L)
            // In a real system, you would fetch the list of admin IDs from a service
            Long adminId = 1L; // Default admin ID
            
            Notification notification = notificationService.createPaymentFailedNotification(
                orderId.toString(), reason, adminId);
            notificationService.sendToUser(notification, adminId);
            
        } catch (NumberFormatException e) {
            System.err.println("Error parsing payment failure message: " + e.getMessage());
        }
    }
}