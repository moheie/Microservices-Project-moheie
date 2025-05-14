package com.example.notificationservice.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker for sending messages to clients
        // via destination prefixes: /topic (for broadcasts) and /user (for user-specific messages)
        config.enableSimpleBroker("/topic", "/user");
        
        // Prefix for messages from clients to the server
        config.setApplicationDestinationPrefixes("/app");
        
        // Prefix for user-specific messages
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the /notifications endpoint for WebSocket connections
        // Allow all origins for development, should be restricted in production
        registry.addEndpoint("/notifications")
                .setAllowedOrigins("*")
                .withSockJS();
                
        // Also add a non-SockJS fallback
        registry.addEndpoint("/notifications")
                .setAllowedOrigins("*");
    }
}
