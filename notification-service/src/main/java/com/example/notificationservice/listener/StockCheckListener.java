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
    
    @RabbitListener(queues = "stock-check")
    public void handleStockCheck(String message) {
        System.out.println("Stock check notification: " + message);
        
        // Parse the message - expecting format: "productId:productName:quantity"
        String[] parts = message.split(":");
        if (parts.length >= 3) {
            String productId = parts[0];
            String productName = parts[1];
            int quantity = Integer.parseInt(parts[2]);
            
            // Notify if stock is low (threshold could be configurable)
            if (quantity < 10) {
                Notification notification = notificationService.createStockNotification(
                        productName, quantity);
                notificationService.sendToSellers(notification);
            }
        }
    }
}
