package com.example.auth.service;

import com.example.auth.model.Role;
import com.example.auth.model.User;
import com.example.auth.utils.Jwt;
import com.example.auth.utils.Security;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Stateless
public class UserService {

    @PersistenceContext(unitName = "auth-service")
    private EntityManager entityManager;

    public Response createUser(String username, String password, String email, String companyName, Role role) {
        try {
            // Validate inputs
            if (username == null || password == null || email == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Username, password, and email are required").build();
            }

            // Validate company name for restaurant representatives
            if (role == Role.RESTAURANT_REPRESENTATIVE) {
                if (companyName == null || companyName.isEmpty()) {
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity("Company name is required for restaurant representatives").build();
                }

                // Check if a representative for the company already exists
                try {
                    entityManager.createNamedQuery("User.findByCompanyName", User.class)
                            .setParameter("companyName", companyName)
                            .getSingleResult();
                    return Response.status(Response.Status.CONFLICT)
                            .entity("A representative for this company already exists").build();
                } catch (NoResultException ignored) {
                    // No representative exists, continue
                }
            }

            // Check if username exists
            try {
                entityManager.createNamedQuery("User.findByUsername", User.class)
                        .setParameter("username", username)
                        .getSingleResult();
                return Response.status(Response.Status.CONFLICT)
                        .entity("Username already exists").build();
            } catch (NoResultException ignored) {
                // Username doesn't exist, continue
            }

            // Check if email exists
            try {
                entityManager.createNamedQuery("User.findByEmail", User.class)
                        .setParameter("email", email)
                        .getSingleResult();
                return Response.status(Response.Status.CONFLICT)
                        .entity("Email already exists").build();
            } catch (NoResultException ignored) {
                // Email doesn't exist, continue
            }

            // Encrypt the password
            String encryptedPassword = Security.encrypt(password);

            // Create and persist user
            User user = new User(username, encryptedPassword, email, companyName, role);
            entityManager.persist(user);
            entityManager.flush();

            return Response.status(Response.Status.CREATED)
                    .entity("User created successfully").build();

        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error creating user: " + e.getMessage()).build();
        }
    }

    public Response authenticateUser(String username, String password) {
        try {
            // Find user by username using named query
            User user;
            try {
                user = entityManager.createNamedQuery("User.findByUsername", User.class)
                        .setParameter("username", username)
                        .getSingleResult();
            } catch (NoResultException e) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Invalid credentials").build();
            }

            // Decrypt and compare passwords
            if (Security.decrypt(user.getPassword()).equals(password)) {
                String token = generateToken(user);
                return Response.ok().entity(token).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Invalid credentials").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Authentication error").build();
        }
    }

    // For admin to list all customers
    public List<User> getAllCustomers() {
        return entityManager.createNamedQuery("User.findByRole", User.class)
                .setParameter("role", Role.CUSTOMER)
                .getResultList();
    }

    // For admin to list all restaurant representatives
    public List<User> getAllRestaurantRepresentatives() {
        return entityManager.createNamedQuery("User.findByRole", User.class)
                .setParameter("role", Role.RESTAURANT_REPRESENTATIVE)
                .getResultList();
    }

    // Generate JWT token
    private String generateToken(User user) {
        return Jwt.generateToken(user);
    }

    public String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}