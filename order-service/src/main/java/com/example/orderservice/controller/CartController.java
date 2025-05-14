package com.example.orderservice.controller;

import com.example.orderservice.model.Cart;
import com.example.orderservice.service.CartService;
import com.example.orderservice.utils.Roles;
import jakarta.ejb.EJB;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/cart")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CartController {

    @Inject
    private CartService cartService;

    @GET
    @Path("/get")
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
            @QueryParam("productId") Long productId, @QueryParam("quantity") int quantity,
            @QueryParam("dishName") String dishName,
            @QueryParam("dishPrice") double dishPrice,
            @QueryParam("companyName") String companyName) {
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
            cartService.addProductsToCart(productId, quantity, dishName, dishPrice, companyName);
            return Response.ok("Product added to cart").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error adding product to cart: " + e.getMessage()).build();
        }
    }

    @DELETE
    @Path("/clear")
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

    @DELETE
    @Path("/remove")
    public Response removeFromCart(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("productId") Long productId, @QueryParam("quantity") int quantity) {

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
            cartService.removeProductFromCart(productId, quantity);
            return Response.ok("Product removed from cart").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error removing product from cart: " + e.getMessage()).build();
        }
    }

    @POST
    @Path("/save")
    public Response saveCart(@HeaderParam("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Valid authentication token required").build();
        }

        String token = authHeader.substring("Bearer ".length());

        try {
            cartService.initializeCart(token);
            cartService.persistCart();
            return Response.ok("Cart saved to database").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error saving cart: " + e.getMessage()).build();
        }
    }
}