package com.example.notificationservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class LogListener {
    @RabbitListener(queues = "log")
    public void handleLogMessage(String message) {
        if (message.contains("_Error")) {
            System.out.println("Admin Notification (Error): " + message);
        }
    }
}