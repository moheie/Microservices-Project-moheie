package com.example.notificationservice.listener;

import com.example.notificationservice.service.NotificationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LogListener {
    
    private final NotificationService notificationService;
    private static final Long ADMIN_USER_ID = 1L; // Default admin user ID
    
    @Autowired
    public LogListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    @RabbitListener(queues = "#{@adminLogQueue.name}")
    public void handleLogs(String message) {
        System.out.println("Log message received: " + message);
        
        // Parse service name, severity and message
        // Format: "ServiceName_Severity:message"
        String[] parts = message.split(":", 2);
        try {
            if (parts.length >= 2) {
                String serviceInfo = parts[0];
                String logMessage = parts[1];
                
                // Parse service name and severity
                String[] serviceInfoParts = serviceInfo.split("_");
                if (serviceInfoParts.length >= 2) {
                    String serviceName = serviceInfoParts[0];
                    String severity = serviceInfoParts[1];
                    
                    // Only notify admins about errors
                    if (severity.equals("Error")) {
                        System.out.println("Notifying admin about error: " + message);
                        
                        // Create and send notification for admin
                        notificationService.sendLogMessage(
                            serviceName,
                            severity,
                            logMessage,
                            ADMIN_USER_ID
                        );
                    } else {
                        // Just log other severities but don't send notifications
                        System.out.println("Logging " + severity.toLowerCase() + 
                            " from " + serviceName + ": " + logMessage);
                    }
                }
            } else {
                System.err.println("Invalid log message format: " + message);
            }
        } catch (Exception e) {
            System.err.println("Error processing log message: " + e.getMessage());
        }
    }
}