package com.example.product.controller;


import com.example.product.utils.Jwt;
import com.example.product.utils.Roles;
import com.example.product.service.DishService;
import jakarta.inject.Inject;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

@Path("/dish")
public class DishController {

    @Inject
    DishService dishService;

    @Path("/create")
    public Response createDish(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("name") String name,
            @QueryParam("description") String description,
            @QueryParam("price") double price,
            @QueryParam("stockCount") int stockCount) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Valid authentication token required").build();
            }

            String token = authHeader.substring("Bearer ".length());
            String role = Jwt.getRole(token);
            if (!role.equals(Roles.RESTAURANT_REPRESENTATIVE.toString())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Unauthorized access").build();
            }
            String companyName = Jwt.getCompany(token);

            if (name == null || name.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Dish name is required").build();
            }

            if (price <= 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Price must be greater than zero").build();
            }

            if (stockCount < 0) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("Stock count cannot be negative").build();
            }

            return dishService.createDish(name, description, price, companyName, stockCount);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error creating dish: " + e.getMessage()).build();
        }
    }
}