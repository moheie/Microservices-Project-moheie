package com.example.product.controller;


import com.example.product.utils.Jwt;
import com.example.product.utils.Roles;
import com.example.product.service.DishService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/dish")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DishController {

    @Inject
    DishService dishService;

    Roles ROLES;

    @Path("/create")
    @POST
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
            if (!role.equals(ROLES.RESTAURANT_REPRESENTATIVE.toString())) {
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
    //TODO: add update and delete methods and get all dishes by company name

    @GET
    @Path("/getDishes")
    public Response viewDishes(@HeaderParam("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Valid authentication token required").build();
            }

            String token = authHeader.substring("Bearer ".length());
            String role = Jwt.getRole(token);
            if (!role.equals(ROLES.RESTAURANT_REPRESENTATIVE.toString())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Unauthorized access").build();
            }

            String companyName = Jwt.getCompany(token);
            return dishService.getDishesByCompanyName(companyName);

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving dishes: " + e.getMessage()).build();
        }
    }

    @PUT
    @Path("/update")
    public Response updateDish(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("dishId") Long dishId,
            @QueryParam("name") String name,
            @QueryParam("description") String description,
            @QueryParam("price") Double price,
            @QueryParam("stockCount") Integer stockCount) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Valid authentication token required").build();
            }

            String token = authHeader.substring("Bearer ".length());
            String role = Jwt.getRole(token);
            if (!role.equals(ROLES.RESTAURANT_REPRESENTATIVE.toString())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Unauthorized access").build();
            }
            String companyName = Jwt.getCompany(token);

            return dishService.updateDish(dishId, name, description, price, stockCount, companyName);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error updating dish: " + e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/delete")
    public Response deleteDish(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("dishId") Long dishId) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Valid authentication token required").build();
            }

            String token = authHeader.substring("Bearer ".length());
            String role = Jwt.getRole(token);
            if (!role.equals(ROLES.RESTAURANT_REPRESENTATIVE.toString())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Unauthorized access").build();
            }
            String companyName = Jwt.getCompany(token);

            return dishService.deleteDish(dishId, companyName);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error deleting dish: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/getSoldDishes")
    public Response getSoldDishes(@HeaderParam("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return Response.status(Response.Status.UNAUTHORIZED)
                        .entity("Valid authentication token required").build();
            }

            String token = authHeader.substring("Bearer ".length());
            String role = Jwt.getRole(token);
            if (!role.equals(ROLES.RESTAURANT_REPRESENTATIVE.toString())) {
                return Response.status(Response.Status.FORBIDDEN)
                        .entity("Unauthorized access").build();
            }
            String companyName = Jwt.getCompany(token);

            return dishService.getSoldDishesByCompanyName(companyName);
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving sold dishes: " + e.getMessage()).build();
        }
    }
}