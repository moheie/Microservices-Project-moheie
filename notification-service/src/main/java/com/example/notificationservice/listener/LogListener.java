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
    
    @RabbitListener(queues = "admin-log")
    public void handleLogs(String message) {
        // Parse service name, severity and message
        // Format from services: "serviceName_severity:message"
        String[] parts = message.split(":", 2);
        String serviceInfo = parts[0];
        String logMessage = parts.length > 1 ? parts[1] : "No details provided";
        
        // Parse service name and severity
        String[] serviceInfoParts = serviceInfo.split("_");
        String serviceName = serviceInfoParts[0];
        String severity = serviceInfoParts.length > 1 ? serviceInfoParts[1] : "Info";
        
        System.out.println("Log received - Service: " + serviceName + ", Severity: " + severity + ", Message: " + logMessage);
        
        // Only notify about errors - using admin ID 1L
        // In a real system, you would fetch the list of admin IDs from a service
        if (severity.equals("Error")) {
            Long adminId = 1L; // Default admin ID
            System.out.println("Notifying admin about error: " + message);
            Notification notification = notificationService.createErrorNotification(serviceName, logMessage, adminId);
            notificationService.sendToUser(notification, adminId);
        } else {
            // Just log other severities but don't send notifications
            System.out.println("Logging " + severity.toLowerCase() + " from " + serviceName + ": " + logMessage);
        }
    }
}