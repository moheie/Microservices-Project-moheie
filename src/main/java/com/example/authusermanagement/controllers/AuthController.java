package com.example.authusermanagement.controllers;

import com.example.authusermanagement.model.Role;
import com.example.authusermanagement.model.User;
import com.example.authusermanagement.service.UserService;
import com.example.authusermanagement.utils.Jwt;

import jakarta.ejb.EJB;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {

    @EJB
    private UserService userService;

    @POST
    @Path("/register/customer")
    public Response registerCustomer(CustomerRegistrationRequest request) {
        return userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                null,
                Role.CUSTOMER
        );
    }

    @POST
    @Path("/register/restaurant")
    public Response registerRestaurantRep(RestaurantRegistrationRequest request) {
        return userService.createUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getCompanyName(),
                Role.RESTAURANT_REPRESENTATIVE
        );
    }

    @POST
    @Path("/login")
    public Response login(LoginRequest request) {
        return userService.authenticateUser(request.getUsername(), request.getPassword());
    }

    @GET
    @Path("/customers")
    public Response getAllCustomers(@HeaderParam("Authorization") String authHeader) {
        try {
            // Extract token from Authorization header
            String token = authHeader.substring("Bearer ".length());

            // Validate token and check if admin
            String role = Jwt.getRole(token);
            if (!role.equals(Role.ADMIN.toString())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Unauthorized access").build();
            }

            List<User> customers = userService.getAllCustomers();
            return Response.ok(customers).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid token").build();
        }
    }

    @GET
    @Path("/restaurants")
    public Response getAllRestaurantReps(@HeaderParam("Authorization") String authHeader) {
        try {
            // Extract token from Authorization header
            String token = authHeader.substring("Bearer ".length());

            // Validate token and check if admin
            String role = Jwt.getRole(token);
            if (!role.equals(Role.ADMIN.toString())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Unauthorized access").build();
            }

            List<User> restaurantReps = userService.getAllRestaurantRepresentatives();
            return Response.ok(restaurantReps).build();
        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid token").build();
        }
    }

    // Request classes for JSON binding
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class CustomerRegistrationRequest {
        private String username;
        private String password;
        private String email;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class RestaurantRegistrationRequest {
        private String username;
        private String password;
        private String email;
        private String companyName;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
    }
}