package com.example.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String ORDER_CONFIRMATION_QUEUE = "order-confirmation";
    public static final String PAYMENT_FAILED_QUEUE = "payment-failed";
    public static final String LOG_EXCHANGE = "log";

    @Bean
    public Queue orderConfirmationQueue() {
        return new Queue(ORDER_CONFIRMATION_QUEUE, false);
    }

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(PAYMENT_FAILED_QUEUE, false);
    }

    @Bean
    public TopicExchange logExchange() {
        return new TopicExchange(LOG_EXCHANGE);
    }
}