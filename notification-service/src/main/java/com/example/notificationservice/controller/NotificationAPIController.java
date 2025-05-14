package com.example.notificationservice.controller;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*") // In production, restrict this to your frontend domain
public class NotificationAPIController {
    
    private final NotificationService notificationService;
    
    @Autowired
    public NotificationAPIController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Get all notifications for a specific user type
     */
    @GetMapping("/by-type/{userType}")
    public ResponseEntity<List<Notification>> getByUserType(@PathVariable String userType) {
        List<Notification> notifications = notificationService.getNotificationsByUserType(userType);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Get all notifications for a specific user ID
     */
    @GetMapping("/by-user/{userId}")
    public ResponseEntity<List<Notification>> getByUserId(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Get unread notifications for a specific user type
     */
    @GetMapping("/unread/by-type/{userType}")
    public ResponseEntity<List<Notification>> getUnreadByUserType(@PathVariable String userType) {
        List<Notification> notifications = notificationService.getUnreadNotificationsByUserType(userType);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Get unread notifications for a specific user ID
     */
    @GetMapping("/unread/by-user/{userId}")
    public ResponseEntity<List<Notification>> getUnreadByUserId(@PathVariable Long userId) {
        List<Notification> notifications = notificationService.getUnreadNotificationsByUserId(userId);
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Mark a notification as read
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Mark all notifications for a user type as read
     */
    @PatchMapping("/read-all/by-type/{userType}")
    public ResponseEntity<Void> markAllAsReadByType(@PathVariable String userType) {
        notificationService.markAllAsRead(userType);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Mark all notifications for a specific user ID as read
     */
    @PatchMapping("/read-all/by-user/{userId}")
    public ResponseEntity<Void> markAllAsReadByUser(@PathVariable Long userId) {
        notificationService.markAllAsReadForUser(userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Create a test notification
     */
    @PostMapping("/test")
    public ResponseEntity<Notification> createTestNotification(@RequestBody Map<String, String> payload) {
        String type = payload.getOrDefault("type", "info");
        String userType = payload.getOrDefault("userType", "admin");
        String title = payload.getOrDefault("title", "Test Notification");
        String message = payload.getOrDefault("message", "This is a test notification");
        Long userId = payload.containsKey("userId") ? Long.parseLong(payload.get("userId")) : null;
        
        Notification notification = new Notification(type, title, message, userType);
        
        if (userId != null) {
            notification.setUserId(userId);
            notificationService.sendToUser(notification, userId);
        } else {
            notificationService.sendToUserType(notification);
        }
        
        return ResponseEntity.ok(notification);
    }
}
