package com.example.orderservice.service;

import com.example.orderservice.config.RabbitMQConfig;
import com.example.orderservice.messaging.NotificationSender;
import com.example.orderservice.model.OrderItem;
import com.example.orderservice.repository.OrderItemRepository;
import com.rabbitmq.client.Channel;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Stateless
public class StockCheckService {
    
    @Inject
    private RabbitMQConfig rabbitMQConfig;
    
    @Inject
    private NotificationSender notificationSender;
    
    @Inject
    private OrderItemRepository orderItemRepository;
    
    private static final int LOW_STOCK_THRESHOLD = 10;
    
    /**
     * Check stock availability for a list of products
     * 
     * @param productIds Array of product IDs to check
     * @param quantities Array of quantities corresponding to the product IDs
     * @return True if all products are in stock, false otherwise
     */
    public boolean checkStock(Long[] productIds, int[] quantities) {
        // In a real implementation, this would make API calls to the product service
        // For now, we'll assume all products are in stock
        
        // Log the stock check operation
        notificationSender.sendLogMessage("Order", "Info", 
            "Checking stock for " + productIds.length + " products");
        
        // For demo purposes, send a stock notification for a random product
        if (productIds.length > 0) {
            // Get product details (in real implementation)
            String productName = "Product #" + productIds[0];
            int stockRemaining = 8; // Mock low stock for demonstration
            
            notificationSender.sendStockCheck(
                productIds[0].toString(),
                productName,
                stockRemaining
            );
        }
        
        return true; // Assume everything is in stock
    }
    
    /**
     * Check if an order meets the minimum charge requirement
     */
    public boolean meetsMinimumCharge(double totalPrice, double minimumCharge) {
        return totalPrice >= minimumCharge;
    }
    
    /**
     * Update stock levels after a purchase
     */
    public void updateStockLevels(List<OrderItem> items) {
        // In a real implementation, this would make API calls to the product service
        // For now, we'll just log the operation
        notificationSender.sendLogMessage("Order", "Info", 
            "Updating stock levels for " + items.size() + " items");
    }
}
