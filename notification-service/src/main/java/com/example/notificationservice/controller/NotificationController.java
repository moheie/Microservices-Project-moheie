package com.example.notificationservice.controller;

import com.example.notificationservice.model.Notification;
import com.example.notificationservice.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

@Controller
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }
    
    /**
     * Handle WebSocket subscription requests
     */
    @MessageMapping("/subscribe")
    public void processSubscription(@Payload Map<String, String> subscription, 
                                   SimpMessageHeaderAccessor headerAccessor) {
        String userType = subscription.get("userType");
        String token = subscription.get("token");
        
        // In a real system, you would validate the token and extract the user ID
        // For now, just store the userType in the session
        headerAccessor.getSessionAttributes().put("userType", userType);
        System.out.println("User with type " + userType + " subscribed to notifications");
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "Notification service is up and running!";
    }
}
