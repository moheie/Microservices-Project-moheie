package com.example.notificationservice.listener;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PaymentFailureListener {
    
    private final NotificationService notificationService;
    private static final Long ADMIN_USER_ID = 1L; // Default admin user ID
    
    @Autowired
    public PaymentFailureListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = "#{paymentFailedQueue.name}")
    public void handlePaymentFailure(String message) {
        System.out.println("Payment Failure Notification: " + message);
        
        // Parse the message - expecting format: "orderId:userId:reason"
        String[] parts = message.split(":", 3);
        try {
            if (parts.length >= 3) {                
                String orderId = parts[0];
                Long customerId = Long.parseLong(parts[1]);
                String reason = parts[2];
                
                // Create and send notification to the customer
                Notification customerNotification = notificationService.createPaymentFailedNotification(
                    orderId, reason, customerId);
                notificationService.sendToUser(customerNotification, customerId);
                
                // Create and send notification to admin
                Notification adminNotification = notificationService.createPaymentFailedNotification(
                    orderId, reason, ADMIN_USER_ID);
                notificationService.sendToUser(adminNotification, ADMIN_USER_ID);
                
                // Log this as an error (will be routed to admins via log exchange)
                notificationService.sendLogMessage(
                    "Payment",
                    "Error",
                    "Payment failed for order " + orderId + " (Customer ID: " + customerId + "): " + reason,
                    ADMIN_USER_ID
                );
            } else {
                System.err.println("Invalid payment failure message format: " + message);
            }
        } catch (Exception e) {
            System.err.println("Error processing payment failure notification: " + e.getMessage());
        }
    }
}