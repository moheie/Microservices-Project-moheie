package com.example.orderservice.controller;

import com.example.orderservice.model.Order;
import com.example.orderservice.service.OrderService;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderController {
    @Inject
    private OrderService orderService;

    @POST
    @Path("/confirm")
    public Response confirmOrder(@HeaderParam("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Valid authentication token required").build();
        }

        String token = authHeader.substring("Bearer ".length());

        try {
            Order order = orderService.createOrderFromCart(token);
            return Response.status(Response.Status.CREATED).entity(order).build();
        } catch (IllegalStateException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error creating order: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/getOrder")
    public Response getOrder(
            @HeaderParam("Authorization") String authHeader,
            @QueryParam("id") Long orderId) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Valid authentication token required").build();
        }

        try {
            Order order = orderService.getOrder(orderId);
            if (order == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Order not found").build();
            }
            return Response.ok(order).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving order: " + e.getMessage()).build();
        }
    }


}