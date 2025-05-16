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
    private Channel channel;
    public static final String ORDER_STOCK_CHECK_QUEUE = "order-stock-check";
    public static final String STOCK_CONFIRMATION_QUEUE = "stock-confirmation";
    public static final String USER_ORDER_CONFIRMATION_QUEUE = "user-order-confirmation";
    public static final String PAYMENT_FAILED_QUEUE = "payment-failed";
    public static final String PAYMENTS_EXCHANGE = "payments-exchange";
    public static final String ADMIN_LOG_EXCHANGE = "admin-log";

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
            channel.exchangeDeclare(PAYMENTS_EXCHANGE, "direct", true);
            channel.exchangeDeclare(ADMIN_LOG_EXCHANGE, "topic", true);

            // Declare queues
            channel.queueDeclare(ORDER_STOCK_CHECK_QUEUE, false, false, false, null);
            channel.queueDeclare(STOCK_CONFIRMATION_QUEUE, false, false, false, null);
            channel.queueDeclare(USER_ORDER_CONFIRMATION_QUEUE, false, false, false, null);
            channel.queueDeclare(PAYMENT_FAILED_QUEUE, false, false, false, null);

            // Bind payment failure queue to exchange
            channel.queueBind(PAYMENT_FAILED_QUEUE, PAYMENTS_EXCHANGE, "PaymentFailed");

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