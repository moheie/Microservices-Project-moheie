package com.example.auth.utils;

import com.example.auth.model.User;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class Jwt {
    // Secret key and expiration defined as constants
    // In production, these should be in a properties file
    private static final String JWT_SECRET = "the-most-secure-secret-key-in-hostory";
    private static final long JWT_EXPIRATION = 86400000; // 24 hours in milliseconds

    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(
            JWT_SECRET.getBytes(StandardCharsets.UTF_8));

    public static String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + JWT_EXPIRATION);

        // Build claims
        Claims claims = Jwts.claims().setSubject(user.getUsername());
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().toString());

        // Add company name if user is a restaurant representative
        if (user.getRole() == com.example.auth.model.Role.RESTAURANT_REPRESENTATIVE
                && user.getCompanyName() != null) {
            claims.put("company", user.getCompanyName());
        }

        // Generate token
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(SECRET_KEY, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(SECRET_KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static String getUsername(String token) {
        return validateToken(token).getSubject();
    }

    public static String getRole(String token) {
        return validateToken(token).get("role", String.class);
    }

    public static String getCompany(String token) {
        return validateToken(token).get("company", String.class);
    }

    public static Long getUserId(String token) {
        return validateToken(token).get("userId", Long.class);
    }
}