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
        
        try {
            String orderId;
            String reason;
            // parse expected format: "orderId:reason" or "orderId"
            // Check if the message contains a colon (for any format)
            if (message.contains(":")) {
                // Split the message
                String[] parts = message.split(":", 2);

                // Format: "orderId:reason"
                if (parts.length == 2) {
                    orderId = parts[0];
                    reason = parts[1];
                    System.out.println("Using format 'orderId:reason'");
                

            
                    // Always send notification to admin
                    Notification adminNotification = notificationService.createPaymentFailedNotification(
                        orderId, reason, ADMIN_USER_ID);
                    notificationService.sendToUser(adminNotification, ADMIN_USER_ID);
                }
            } else {
                // Fallback to default format: "orderId"
                orderId = message;
                reason = "Payment failed for order ID: " + orderId;
                System.out.println("Using default format 'orderId'");
                
                // Create and send notification to admin
                Notification adminNotification = notificationService.createPaymentFailedNotification(
                    orderId, reason, ADMIN_USER_ID);
                notificationService.sendToUser(adminNotification, ADMIN_USER_ID);
            }

        } catch (Exception e) {
            System.err.println("Error processing payment failure notification: " + e.getMessage());
            e.printStackTrace();
            
            // Try to send at least a basic admin notification about the error
            try {
                Notification errorNotification = notificationService.createPaymentFailedNotification(
                    "unknown", "Error processing payment failure: " + e.getMessage(), ADMIN_USER_ID);
                notificationService.sendToUser(errorNotification, ADMIN_USER_ID);
            } catch (Exception ignored) {
                // Last resort logging
                System.err.println("Failed to send error notification: " + ignored.getMessage());
            }
        }
    }
}