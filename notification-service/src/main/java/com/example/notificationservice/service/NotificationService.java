package com.example.notificationservice.service;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    
    public NotificationService(SimpMessagingTemplate messagingTemplate, 
                              NotificationRepository notificationRepository) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
    }
      /**
     * Send a notification to all users of a specific type
     */
    public void sendToUserType(Notification notification) {
        // Save notification to database
        notificationRepository.save(notification);
        
        // Send via WebSocket
        messagingTemplate.convertAndSend("/topic/" + notification.getUserType(), notification);
        System.out.println("Sent notification to " + notification.getUserType() + ": " + notification.getTitle());
    }
    
    /**
     * Send a notification to a specific user by ID
     */
    public void sendToUser(Notification notification, Long userId) {
        // Save notification to database
        notificationRepository.save(notification);
        
        // Send via WebSocket
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/notifications",
            notification
        );
        System.out.println("Sent notification to user " + userId + ": " + notification.getTitle());
    }
    
    /**
     * Get all notifications for a specific user type
     */
    public List<Notification> getNotificationsByUserType(String userType) {
        return notificationRepository.findByUserType(userType);
    }
    
    /**
     * Get all notifications for a specific user ID
     */
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserId(userId);
    }
    
    /**
     * Get unread notifications for a specific user type
     */
    public List<Notification> getUnreadNotificationsByUserType(String userType) {
        return notificationRepository.findByUserTypeAndReadFalse(userType);
    }
    
    /**
     * Get unread notifications for a specific user ID
     */
    public List<Notification> getUnreadNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdAndReadFalse(userId);
    }
    
    /**
     * Mark a notification as read
     */
    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }
    
    /**
     * Mark all notifications for a user type as read
     */
    public void markAllAsRead(String userType) {
        List<Notification> notifications = notificationRepository.findByUserTypeAndReadFalse(userType);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }
    
    /**
     * Mark all notifications for a specific user ID as read
     */
    public void markAllAsReadForUser(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadFalse(userId);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }
    
    /**
     * Send a notification to all admin users
     */
    public void sendToAdmins(Notification notification) {
        notification.setUserType("admin");
        sendToUserType(notification);
    }
    
    /**
     * Send a notification to all sellers
     */
    public void sendToSellers(Notification notification) {
        notification.setUserType("seller");
        sendToUserType(notification);
    }
    
    /**
     * Send a notification to all customers
     */
    public void sendToCustomers(Notification notification) {
        notification.setUserType("customer");
        sendToUserType(notification);
    }
    
    /**
     * Create an error notification for admins
     */
    public Notification createErrorNotification(String source, String errorMessage) {
        return new Notification(
            "error",
            source + " Error",
            errorMessage,
            "admin"
        );
    }
    
    /**
     * Create a payment failure notification
     */
    public Notification createPaymentFailedNotification(String orderId, String reason) {
        return new Notification(
            "payment",
            "Payment Failed",
            "Order #" + orderId + " payment failed: " + reason,
            "admin"
        );
    }
    
    /**
     * Create an order status notification for customers
     */
    public Notification createOrderStatusNotification(String orderId, String status, Long userId) {
        return new Notification(
            "order",
            "Order " + status,
            "Your order #" + orderId + " is now " + status,
            "customer",
            userId
        );
    }
    
    /**
     * Create an order notification for sellers
     */
    public Notification createSellerOrderNotification(String orderId, String status) {
        return new Notification(
            "order",
            "New Order",
            "Order #" + orderId + " has been " + status,
            "seller"
        );
    }
    
    /**
     * Create a stock notification for sellers
     */
    public Notification createStockNotification(String productName, int quantity) {
        return new Notification(
            "stock",
            "Low Stock Alert",
            productName + " is running low on stock. Only " + quantity + " left.",
            "seller"
        );
    }
}
