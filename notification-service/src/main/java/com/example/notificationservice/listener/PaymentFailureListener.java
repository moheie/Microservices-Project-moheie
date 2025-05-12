package com.example.notificationservice.listener;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class PaymentFailureListener {
    @RabbitListener(queues = "payment-failed")
    public void handlePaymentFailure(String message) {
        System.out.println("Payment Failure Notification: " + message);
    }
}