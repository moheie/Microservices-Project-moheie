package com.example.notificationservice.listener;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StockCheckListener {
    
    private final NotificationService notificationService;
    

    
    @Autowired
    public StockCheckListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @RabbitListener(queues = "seller-stock-check")
    public void handleStockCheck(String message) {
        System.out.println("Stock check notification: " + message);

        // Parse the message - expecting format: "productName:quantity:sellerCompanyName[:sellerId]"
        String[] parts = message.split(":");
        if (parts.length >= 3) {
            try {                
                String productName = parts[0];
                int quantity = Integer.parseInt(parts[1]);
                String sellerCompanyName = parts[2];
                Long sellerId = Long.parseLong(parts[3]);                // Create stock alert notification
                Notification notification = notificationService.createStockAlertNotification(
                    productName, 
                    quantity,
                    sellerId
                );
                
                // Send directly to the seller
                notificationService.sendToUser(notification, sellerId);

                // If stock is critically low (less than 5), send to admin
                if (quantity < 5) {
                    // Log system message for admin
                    notificationService.sendLogMessage(
                        "Stock",
                        "Error",
                        String.format("CRITICAL: Product '%s' from %s has very low stock: %d units remaining", 
                            productName, sellerCompanyName, quantity),
                        sellerId
                    );

                    // Create admin notification
                    Notification adminNotification = notificationService.createStockAlertNotification(
                        productName,
                        quantity,
                        1L // Assuming 1L is the admin user ID
                    );
                    notificationService.sendToUser(adminNotification, 1L);
                }
                
            } catch (NumberFormatException e) {
                System.err.println("Error parsing stock check message: " + e.getMessage());
            }
        } else {
            System.err.println("Invalid stock check message format: " + message);
        }
    }
}
