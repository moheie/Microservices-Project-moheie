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
    
    @Bean
    public DirectExchange paymentsExchange() {
        return ExchangeBuilder.directExchange(PAYMENTS_EXCHANGE)
                .durable(true)
                .build();
    }
    
    @Bean
    public TopicExchange logExchange() {
        return ExchangeBuilder.topicExchange(ADMIN_LOG_EXCHANGE)
                .durable(true)
                .build();
    }
    
    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(paymentsExchange())
                .with("PaymentFailed");
    }   

    @Bean
    public Binding orderConfirmationLogBinding() {
        return BindingBuilder.bind(orderConfirmationQueue())
                .to(logExchange())
                .with("Order_*");
    }
    
    @Bean
    public Binding sellerStockLogBinding() {
        return BindingBuilder.bind(sellerStockCheckQueue())
                .to(logExchange())
                .with("Stock_*");
    }

}