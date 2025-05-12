package com.example.notificationservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OrderConfirmationListener {
    @RabbitListener(queues = "order-confirmation")
    public void handleOrderConfirmation(String message) {
        System.out.println("Order Confirmation: " + message);
    }
}