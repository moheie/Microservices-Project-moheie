package com.example.authusermanagement.service;

import com.example.authusermanagement.model.Role;
import com.example.authusermanagement.model.User;
import com.example.authusermanagement.utils.Security;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Stateless
public class UserService {

    @PersistenceContext
    private EntityManager entityManager;

    public Response createUser(String username, String password, String email, String companyName, Role role) {
        try {
            // Check if the username already exists
            TypedQuery<User> query = entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);
            try {
                query.getSingleResult();
                return Response.status(Response.Status.CONFLICT).entity("User already exists").build();
            } catch (NoResultException e) {
                // Username is available, continue with user creation
            }

            // Encrypt the password
            String encryptedPassword = Security.encrypt(password);
            User user = new User(username, encryptedPassword, email, companyName, role);

            // Generate ID if not set
            if (user.getId() == null) {
                user.setId(UUID.randomUUID().toString());
            }

            // Persist the user
            entityManager.persist(user);
            entityManager.flush();

            return Response.status(Response.Status.CREATED).entity("User created successfully").build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error creating user: " + e.getMessage()).build();
        }
    }

    public Response authenticateUser(String username, String password) {
        try {
            TypedQuery<User> query = entityManager.createQuery(
                    "SELECT u FROM User u WHERE u.username = :username", User.class);
            query.setParameter("username", username);

            User user;
            try {
                user = query.getSingleResult();
            } catch (NoResultException e) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
            }

            // Decrypt and compare passwords
            if (Security.decrypt(user.getPassword()).equals(password)) {
                // Generate JWT token (we'll implement this next)
                String token = generateToken(user);
                return Response.ok().entity(token).build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).entity("Invalid credentials").build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Authentication error").build();
        }
    }

    // Method to generate random password for company reps
    public String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    // For admin to list all customers
    public List<User> getAllCustomers() {
        return entityManager.createQuery("SELECT u FROM User u WHERE u.role = :role", User.class)
                .setParameter("role", Role.CUSTOMER)
                .getResultList();
    }

    // For admin to list all restaurant representatives
    public List<User> getAllRestaurantRepresentatives() {
        return entityManager.createQuery("SELECT u FROM User u WHERE u.role = :role", User.class)
                .setParameter("role", Role.RESTAURANT_REPRESENTATIVE)
                .getResultList();
    }

    // Generate JWT token (simplified - in a real app, use a proper JWT library)
    private String generateToken(User user) {
        // This is a placeholder - you should use a proper JWT library like jjwt
        return "token_for_" + user.getUsername() + "_" + System.currentTimeMillis();
    }
}