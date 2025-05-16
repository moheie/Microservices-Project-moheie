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

    @GetMapping("/sendLog")
    @ResponseBody
    public String sendLogMessage(String service, String severity, String message) {
        try {
            notificationService.sendLogMessage(service, severity, message, 1L);
            return "Log message sent successfully!";
        } catch (Exception e) {
            return "Failed to send log message: " + e.getMessage();
        }
    }

    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "Notification service is up and running!";
    }
}
