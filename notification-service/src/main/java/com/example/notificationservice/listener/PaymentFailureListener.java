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
    }    @RabbitListener(queues = "#{paymentFailedQueue.name}")
    public void handlePaymentFailure(String message) {
        System.out.println("Payment Failure Notification: " + message);
        
        try {
            String orderId;
            String reason;
            Long customerId = null;
            
            // Check if the message contains a colon (for any format)
            if (message.contains(":")) {
                // Split the message
                String[] parts = message.split(":", 3);
                
                // Format: "orderId:userId:reason"
                if (parts.length >= 3) {                
                    orderId = parts[0];
                    try {
                        customerId = Long.parseLong(parts[1]);
                    } catch (NumberFormatException e) {
                        // If userId is not a number, treat as part of reason
                        reason = parts[1] + ":" + parts[2];
                        System.out.println("Could not parse userId, treating as part of reason: " + reason);
                    }
                    reason = parts[2];
                } 
                // Format: "orderId:reason"
                else if (parts.length == 2) {
                    orderId = parts[0];
                    reason = parts[1];
                    System.out.println("Using format 'orderId:reason' - no userId provided");
                }
                else {
                    // Invalid format with colon but not enough parts
                    orderId = "unknown";
                    reason = message;
                }
            } else {
                // No colon - treat entire message as the reason
                orderId = "unknown";
                reason = message;
            }
            
            // For cases where we couldn't extract a customer ID, use a default
            if (customerId == null) {
                // Send notification only to admin, not to a specific customer
                System.out.println("No valid customer ID found, sending only to admin");
            } else {
                // Send notification to the customer
                Notification customerNotification = notificationService.createPaymentFailedNotification(
                    orderId, reason, customerId);
                notificationService.sendToUser(customerNotification, customerId);
            }
            
            // Always send notification to admin
            Notification adminNotification = notificationService.createPaymentFailedNotification(
                orderId, reason, ADMIN_USER_ID);
            notificationService.sendToUser(adminNotification, ADMIN_USER_ID);
            
            // Log this as an error (will be routed to admins via log exchange)
            String customerInfo = customerId != null ? " (Customer ID: " + customerId + ")" : "";
            notificationService.sendLogMessage(
                "Payment",
                "Error",
                "Payment failed for order " + orderId + customerInfo + ": " + reason,
                ADMIN_USER_ID
            );
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