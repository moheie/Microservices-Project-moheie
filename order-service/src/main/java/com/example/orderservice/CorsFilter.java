package com.example.orderservice;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
public class CorsFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        responseContext.getHeaders().add("Access-Control-Allow-Origin", "http://localhost:8081"); // Restrict to frontend origin
        responseContext.getHeaders().add("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization"); // Standardize header casing
        responseContext.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD"); // Ensure OPTIONS is allowed
        responseContext.getHeaders().add("Access-Control-Allow-Credentials", "true"); // Allow credentials
        responseContext.getHeaders().add("Access-Control-Max-Age", "3600"); // Reduce max age for preflight caching
    }
}
