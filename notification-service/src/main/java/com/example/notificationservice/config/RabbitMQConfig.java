package com.example.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // Queue names
    public static final String SELLER_STOCK_CHECK_QUEUE = "seller-stock-check";
    public static final String USER_ORDER_CONFIRMATION_QUEUE = "user-order-confirmation";    
    public static final String PAYMENT_FAILED_QUEUE = "payment-failed";
    public static final String LOG_QUEUE = "admin-log";
    
    // Exchange names
    public static final String PAYMENTS_EXCHANGE = "payments-exchange";
    public static final String ADMIN_LOG_EXCHANGE = "admin-log";
    
    @Bean
    public Queue orderConfirmationQueue() {
        return QueueBuilder.nonDurable(USER_ORDER_CONFIRMATION_QUEUE).build();
    }
    
    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.nonDurable(PAYMENT_FAILED_QUEUE).build();
    }

    @Bean
    public Queue sellerStockCheckQueue() {
        return QueueBuilder.nonDurable(SELLER_STOCK_CHECK_QUEUE).build();
    }
    
    // Admin log queue for all error logs
    @Bean
    public Queue adminLogQueue() {
        return QueueBuilder.durable(LOG_QUEUE).build();
    }
    
    // Direct exchange for payment events
    @Bean
    public DirectExchange paymentsExchange() {
        return ExchangeBuilder.directExchange(PAYMENTS_EXCHANGE)
                .durable(true)
                .build();
    }
    
    // Topic exchange for logs with different routing keys based on severity
    @Bean
    public TopicExchange logExchange() {
        return ExchangeBuilder.topicExchange(ADMIN_LOG_EXCHANGE)
                .durable(true)
                .build();
    }
    
    // Bind payment-failed queue to the payments exchange with PaymentFailed routing key
    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(paymentsExchange())
                .with("PaymentFailed");
    }
    
    // Bind the admin log queue to the log exchange with error severity pattern
    @Bean
    public Binding errorLogBinding() {
        // Bind to log patterns ending with _Error (e.g., Order_Error, Inventory_Error)
        return BindingBuilder.bind(adminLogQueue())
                .to(logExchange())
                .with("*_Error");
    }
}