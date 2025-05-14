package com.example.notificationservice.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // Queue names
    public static final String ORDER_CONFIRMATION_QUEUE = "order-confirmation";
    public static final String PAYMENT_FAILED_QUEUE = "payment-failed";
    public static final String ORDER_STATUS_QUEUE = "order-status";
    public static final String STOCK_CHECK_QUEUE = "stock-check";
    
    // Exchange names
    public static final String LOG_EXCHANGE = "log";
    public static final String PAYMENTS_EXCHANGE = "payments-exchange";
    
    @Bean
    public Queue orderConfirmationQueue() {
        return new Queue(ORDER_CONFIRMATION_QUEUE, true);
    }

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue(PAYMENT_FAILED_QUEUE, true);
    }
    
    @Bean
    public Queue orderStatusQueue() {
        return new Queue(ORDER_STATUS_QUEUE, true);
    }
    
    @Bean
    public Queue stockCheckQueue() {
        return new Queue(STOCK_CHECK_QUEUE, true);
    }

    // Topic exchange for logs with different routing keys based on severity
    @Bean
    public TopicExchange logExchange() {
        return new TopicExchange(LOG_EXCHANGE);
    }
    
    // Direct exchange for payment events
    @Bean
    public DirectExchange paymentsExchange() {
        return new DirectExchange(PAYMENTS_EXCHANGE);
    }
    
    // Bind payment-failed queue to the payments exchange with PaymentFailed routing key
    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(paymentsExchange())
                .with("PaymentFailed");
    }
    
    // Bind log queues by severity patterns
    @Bean
    public Binding errorLogBinding() {
        // Bind to log patterns ending with _Error
        return BindingBuilder.bind(new Queue("error-logs", true))
                .to(logExchange())
                .with("*_Error");
    }
}