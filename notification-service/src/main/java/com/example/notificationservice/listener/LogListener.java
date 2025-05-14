package com.example.notificationservice.listener;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogListener {
    
    private final NotificationService notificationService;
    
    @Autowired
    public LogListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @RabbitListener(queues = "error-logs")
    public void handleErrorLogs(String message) {
        System.out.println("Admin Notification (Error): " + message);
        
        // Parse service name and error message
        String[] parts = message.split(":", 2);
        String source = parts[0].replace("_Error", "");
        String errorMessage = parts.length > 1 ? parts[1] : "Unknown error";
        
        // Create and send notification
        Notification notification = notificationService.createErrorNotification(source, errorMessage);
        notificationService.sendToAdmins(notification);
    }
}