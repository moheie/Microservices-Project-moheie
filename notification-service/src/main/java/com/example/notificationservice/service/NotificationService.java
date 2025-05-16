package com.example.notificationservice.service;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.repository.NotificationRepository;

import org.aspectj.weaver.ast.Not;
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

    public void sendToUser(Notification notification, Long userId) {
        notification.setUserId(userId);
        notificationRepository.save(notification);
        messagingTemplate.convertAndSendToUser(
            userId.toString(),
            "/notifications",
            notification
        );
    }

    public List<Notification> getNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserId(userId);
    }

    public List<Notification> getUnreadNotificationsByUserId(Long userId) {
        return notificationRepository.findByUserIdAndReadFalse(userId);
    }

    public void markAsRead(String notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.setRead(true);
            notificationRepository.save(notification);
        });
    }

    public void markAllAsReadForUser(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndReadFalse(userId);
        notifications.forEach(notification -> notification.setRead(true));
        notificationRepository.saveAll(notifications);
    }
    

    public Notification createPaymentFailedNotification(String orderId, String reason, Long userId) {
        return new Notification(
            "payment",
            "Payment Failed - Order " + orderId,
            reason,
            userId
        );
    }
    

    public void sendLogMessage(String service, String severity, String message, Long userId) {
        if (severity.equals("Error")) {
            Notification notification = new Notification(
                "error",
                service + " Error",
                message,
                userId
            );
            sendToUser(notification, userId);
        }
        // Only Error severity notifications are sent to users
        // Other severities are just logged in the system
    }

    public Notification createStockAlertNotification(String productName, int quantity, Long userId) {
        return new Notification(
            "stock",
            "Low Stock Alert",
            "Product " + productName + " is running low (quantity: " + quantity + ")",
            userId
        );
    }

    public Notification createOrderStatusNotification(String orderId, String status, Long userId) {
        return new Notification(
            "order",
            "Order " + orderId + " Status Update", status,
            userId);
    }
    
    public void createNotification(Notification notification) {
        notificationRepository.save(notification);
        // Send to all users (or specific users if needed)
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }
}
