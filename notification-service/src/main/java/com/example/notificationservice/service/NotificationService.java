package com.example.notificationservice.service;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    
    @Autowired
    public NotificationService(SimpMessagingTemplate messagingTemplate, 
                             NotificationRepository notificationRepository) {
        this.messagingTemplate = messagingTemplate;
        this.notificationRepository = notificationRepository;
    }
    
    /**
     * Send a notification to a specific user by ID
     */
    public void sendToUser(Notification notification, Long userId) {
        notification.setUserId(userId);
        notificationRepository.save(notification);
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/notifications",
            notification
        );
    }
    
    /**
     * Get all notifications for a specific user ID
     */
    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserId(userId);
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
     * Mark all notifications for a specific user ID as read
     */
    public void markAllAsReadForUser(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadFalse(userId);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }
    
    /**
     * Create an error notification
     */
    public Notification createErrorNotification(String source, String errorMessage, Long userId) {
        return new Notification("error", source + " Error", errorMessage, userId);
    }
    
    /**
     * Create a payment failure notification
     */
    public Notification createPaymentFailedNotification(String orderId, String reason, Long userId) {
        return new Notification("payment", 
            "Payment Failed - Order " + orderId,
            "Payment failed: " + reason,
            userId);
    }
    
    /**
     * Create an order status notification
     */
    public Notification createOrderStatusNotification(String orderId, String status, Long userId) {
        return new Notification("order",
            "Order " + orderId + " Status Update",
            "Your order status has been updated to: " + status,
            userId);
    }
}
