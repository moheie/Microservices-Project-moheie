package com.example.orderservice.config;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Singleton
@Startup
public class RabbitMQConfig {
    private Connection connection;
    private Channel channel;    public static final String ORDER_STOCK_CHECK_QUEUE = "order-stock-check";
    public static final String STOCK_CONFIRMATION_QUEUE = "stock-confirmation";
    public static final String ORDER_CONFIRMATION_QUEUE = "order-confirmation";
    public static final String PAYMENT_FAILED_QUEUE = "payment-failed";
    public static final String STOCK_CHECK_QUEUE = "stock-check";
    public static final String LOG_EXCHANGE = "log";
    public static final String PAYMENTS_EXCHANGE = "payments-exchange";

    @PostConstruct
    public void init() {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");  // Docker service name
            factory.setPort(5672);
            factory.setUsername("guest");
            factory.setPassword("guest");

            connection = factory.newConnection();
            channel = connection.createChannel();

            // Declare exchanges
            channel.exchangeDeclare(LOG_EXCHANGE, "topic", true);
            channel.exchangeDeclare(PAYMENTS_EXCHANGE, "direct", true);

            // Declare queues
            channel.queueDeclare(ORDER_STOCK_CHECK_QUEUE, true, false, false, null);
            channel.queueDeclare(STOCK_CONFIRMATION_QUEUE, true, false, false, null);
            channel.queueDeclare(ORDER_CONFIRMATION_QUEUE, true, false, false, null);
            channel.queueDeclare(PAYMENT_FAILED_QUEUE, true, false, false, null);
            channel.queueDeclare(STOCK_CHECK_QUEUE, true, false, false, null);
            
            // Declare error logs queue
            channel.queueDeclare("error-logs", true, false, false, null);
            
            // Bind queues to exchanges
            channel.queueBind(PAYMENT_FAILED_QUEUE, PAYMENTS_EXCHANGE, "PaymentFailed");
            channel.queueBind("error-logs", LOG_EXCHANGE, "*_Error");
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException("Failed to initialize RabbitMQ connection", e);
        }
    }

    public Channel getChannel() {
        return channel;
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (channel != null && channel.isOpen()) {
                channel.close();
            }
            if (connection != null && connection.isOpen()) {
                connection.close();
            }
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}