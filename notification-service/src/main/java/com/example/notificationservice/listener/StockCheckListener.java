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
        
        // Parse the message - expecting format: "productName:quantity:sellerCompanyName"
        String[] parts = message.split(":");
        if (parts.length >= 3) {
            try {
                String productName = parts[0];
                int quantity = Integer.parseInt(parts[1]);
                String sellerCompanyName = parts[2];
                
                // For now, we'll use the company name as the seller ID until proper mapping is implemented
                // In a real system, you would have a service to look up the seller ID by company name
                Long sellerId = (long) sellerCompanyName.hashCode(); // Temporary solution
                
                // Notify if stock is low (threshold could be configurable)
                if (quantity < 10) {
                    Notification notification = new Notification(
                        "stock",
                        "Low Stock Alert",
                        "Product " + productName + " is running low (quantity: " + quantity + ")",
                        sellerId
                    );
                    notificationService.sendToUser(notification, sellerId);
                    
                    // Also log this as a warning
                    System.out.println("Sent low stock notification to seller: " + sellerCompanyName + 
                        " for product: " + productName + " (quantity: " + quantity + ")");
                }
            } catch (NumberFormatException e) {
                System.err.println("Error parsing stock check message: " + e.getMessage());
            }
        } else {
            System.err.println("Invalid stock check message format: " + message);
        }
    }
}
