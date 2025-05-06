package com.example.auth.controllers;

import com.example.auth.DTO.RegistrationResponse;
import com.example.auth.model.Role;
import com.example.auth.model.User;
import com.example.auth.service.UserService;
import com.example.auth.utils.Jwt;

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
    public Response registerCustomer(
            @QueryParam("username") String username,
            @QueryParam("password") String password,
            @QueryParam("email") String email) {
        return userService.createUser(
                username,
                password,
                email,
                null,
                Role.CUSTOMER
        );
    }

    @POST
    @Path("/login")
    public Response login(
            @QueryParam("username") String username,
            @QueryParam("password") String password) {
        return userService.authenticateUser(username, password);
    }

    @POST
    @Path("/register/restaurant")
    public Response registerRestaurantRep(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("username") String username,
            @QueryParam("email") String email,
            @QueryParam("companyName") String companyName) {
        try {
            // Check if Authorization header exists
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Authentication required").build();
            }

            // Extract token from Authorization header
            String token = authHeader.substring("Bearer ".length());

            // Validate token and check if admin
            String role = Jwt.getRole(token);
            if (!role.equals(Role.ADMIN.toString())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Only administrators can create restaurant representatives").build();
            }

            // Generate a random password
            String generatedPassword = userService.generateRandomPassword();

            // Create the user with the generated password
            Response response = userService.createUser(
                    username,
                    generatedPassword,
                    email,
                    companyName,
                    Role.RESTAURANT_REPRESENTATIVE
            );

            // If user creation was successful, return the generated password
            if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                return Response.status(Response.Status.CREATED)
                        .entity(new RegistrationResponse("Restaurant representative created successfully", generatedPassword))
                        .build();
            }

            // If there was an error, return the original response
            return response;

        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Invalid token or authorization error: " + e.getMessage()).build();
        }
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

}