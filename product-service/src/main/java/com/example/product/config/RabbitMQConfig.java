package com.example.product.config;

import com.example.product.dto.StockCheckRequest;
import com.example.product.dto.StockConfirmationResponse;
import com.example.product.model.Dish;
import com.example.product.service.DishService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.rabbitmq.client.DeliverCallback;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Singleton
@Startup
public class RabbitMQConfig {
    private Connection connection;
    private Channel channel;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PersistenceContext(unitName = "product-service")
    private EntityManager entityManager;

    @Inject
    private DishService dishService;

    public static final String STOCK_CONFIRMATION_QUEUE = "stock-confirmation";
    private static final String ORDER_STOCK_CHECK_QUEUE = "order-stock-check";

    public static final String SELLER_STOCK_CHECK_QUEUE = "seller-stock-check";
    public static final String ADMIN_LOG_EXCHANGE = "admin-log";

    @PostConstruct
    public void init() {
        try {
            // Configure ObjectMapper
            objectMapper.registerModule(new JavaTimeModule());

            ConnectionFactory factory = new ConnectionFactory();
            factory.setHost("localhost");
            factory.setPort(5672);
            factory.setUsername("guest");
            factory.setPassword("guest");

            connection = factory.newConnection();
            channel = connection.createChannel();

            // Declare exchanges
            channel.exchangeDeclare(ADMIN_LOG_EXCHANGE, "topic", true);

            // Declare queues
            channel.queueDeclare(ORDER_STOCK_CHECK_QUEUE, false, false, false, null);
            channel.queueDeclare(STOCK_CONFIRMATION_QUEUE, false, false, false, null);
            channel.queueDeclare(SELLER_STOCK_CHECK_QUEUE, false, false, false, null);

            // Bind queues to exchanges
            channel.queueBind(SELLER_STOCK_CHECK_QUEUE, ADMIN_LOG_EXCHANGE, "Stock_*");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                processStockCheckRequest(message);
            };

            channel.basicConsume(ORDER_STOCK_CHECK_QUEUE, true, deliverCallback, consumerTag -> {});

            System.out.println("\u001B[32m === RABBITMQ CONFIG INITIALIZED SUCCESSFULLY === \u001B[0m");
        } catch (IOException | TimeoutException e) {
            System.err.println("\u001B[31m Failed to initialize RabbitMQ connection: " + e.getMessage() + " \u001B[0m");
            throw new RuntimeException("Failed to initialize RabbitMQ connection", e);
        }
    }

    public Channel getChannel() {
        return channel;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    private void processStockCheckRequest(String message) {
        try {
            System.out.println("\u001B[34m Received stock check request: " + message + " \u001B[0m");

            // Parse the message from JSON to StockCheckRequest object
            StockCheckRequest request = objectMapper.readValue(message, StockCheckRequest.class);

            System.out.println("\u001B[34m Deserialized request: " + request + " \u001B[0m");

            Long orderId = request.getOrderId();
            Map<Long, Integer> productQuantities = request.getProductQuantities();

            if (orderId == null || productQuantities == null) {
                throw new IllegalArgumentException("Invalid request format: orderId or productQuantities is null");
            }

            // Convert to map with Long values
            Map<Long, Long> productCounts = new HashMap<>();
            for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
                productCounts.put(entry.getKey(), entry.getValue().longValue());
            }

            System.out.println("\u001B[35m Processing order: \u001B[0m " + orderId +
                    " with products: " + productCounts);

            boolean allInStock = checkStock(productCounts);
            double totalPrice = calculateTotalPrice(productCounts);

            // Create response object
            StockConfirmationResponse response = new StockConfirmationResponse(
                    orderId,
                    allInStock,
                    totalPrice
            );

            // Serialize to JSON and send
            String jsonResponse = objectMapper.writeValueAsString(response);
            System.out.println("\u001B[34m Sending response: " + jsonResponse + " \u001B[0m");

            channel.basicPublish(
                    "",  // Default exchange
                    STOCK_CONFIRMATION_QUEUE,
                    null,
                    jsonResponse.getBytes(StandardCharsets.UTF_8)
            );

            if (allInStock) {
                dishService.decreaseStock(productCounts);
                System.out.println("\u001B[35m Stock decreased for order ID: \u001B[0m " + orderId);
            } else {
                System.out.println("\u001B[31m Insufficient stock for order ID: \u001B[0m " + orderId);
            }
        } catch (Exception e) {
            System.err.println("\u001B[31m Error processing stock check request: " + e.getMessage() + " \u001B[0m");
            e.printStackTrace();

            try {
                // Send error to admin log exchange
                if (channel != null) {
                    String errorMessage = "Product_Error:" + e.getMessage();
                    channel.basicPublish(ADMIN_LOG_EXCHANGE, "Product_Error", null,
                            errorMessage.getBytes(StandardCharsets.UTF_8));
                    System.out.println("\u001B[31m Sent error to admin log: " + errorMessage + "\u001B[0m");
                }
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        }
    }

    private boolean checkStock(Map<Long, Long> productCounts) {
        for (Map.Entry<Long, Long> entry : productCounts.entrySet()) {
            Long productId = entry.getKey();
            Long quantity = entry.getValue();

            System.out.println("\u001B[35m Checking stock for product ID: \u001B[0m " +
                    productId + " (quantity needed: " + quantity + ")");

            Dish dish = entityManager.find(Dish.class, productId);
            if (dish == null || dish.getStockCount() < quantity) {
                System.out.println("\u001B[31m Product " + productId +
                        " insufficient stock: available=" +
                        (dish != null ? dish.getStockCount() : 0) +
                        ", needed=" + quantity + "\u001B[0m");
                return false;
            }
        }
        return true;
    }

    private double calculateTotalPrice(Map<Long, Long> productCounts) {
        double totalPrice = 0.0;
        for (Map.Entry<Long, Long> entry : productCounts.entrySet()) {
            Long productId = entry.getKey();
            Long quantity = entry.getValue();

            Dish dish = entityManager.find(Dish.class, productId);
            if (dish != null) {
                double itemTotal = dish.getPrice() * quantity;
                totalPrice += itemTotal;
                System.out.println("\u001B[36m Product " + productId +
                        ": $" + dish.getPrice() + " x " + quantity +
                        " = $" + itemTotal + "\u001B[0m");
            }
        }
        System.out.println("\u001B[36m Total price: $" + totalPrice + "\u001B[0m");
        return totalPrice;
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