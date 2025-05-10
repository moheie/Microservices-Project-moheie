package com.example.orderservice.controller;

import com.example.orderservice.model.Cart;
import com.example.orderservice.service.CartService;
import com.example.orderservice.utils.Roles;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/cart")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CartController {
    @EJB
    private CartService cartService;

    @GET
    public Response getCart(@HeaderParam("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Valid authentication token required").build();
        }

        String token = authHeader.substring("Bearer ".length());

        try {
            cartService.initializeCart(token);
            Cart cart = cartService.getCurrentCart();
            return Response.ok(cart).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving cart: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/add")
    public Response addToCart(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("productId") Long productId) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Valid authentication token required").build();
        }

        String token = authHeader.substring("Bearer ".length());

        if (productId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("Product ID is required").build();
        }

        try {
            cartService.initializeCart(token);
            cartService.addProductToCart(productId);
            return Response.ok("Product added to cart").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error adding product to cart: " + e.getMessage()).build();
        }
    }

    @DELETE
    public Response clearCart(@HeaderParam("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Valid authentication token required").build();
        }

        String token = authHeader.substring("Bearer ".length());

        try {
            cartService.initializeCart(token);
            cartService.clearCart();
            return Response.ok("Cart cleared").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error clearing cart: " + e.getMessage()).build();
        }
    }
}