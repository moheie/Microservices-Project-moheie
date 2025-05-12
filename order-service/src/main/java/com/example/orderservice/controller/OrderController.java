package com.example.orderservice.controller;

import com.example.orderservice.dto.CompanyOrderDTO;
import com.example.orderservice.model.Order;
import com.example.orderservice.service.OrderService;
import jakarta.ejb.EJB;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

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

    @GET
    @Path("/getOrders")
    public Response getOrders(@HeaderParam("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Valid authentication token required").build();
        }

        String token = authHeader.substring("Bearer ".length());

        try {
            List<Order> orders = orderService.getOrders(token);
            if (orders.isEmpty()) {
                return Response.status(Response.Status.OK)
                        .entity("No orders found for this user").build();
            }
            return Response.ok(orders).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving orders: " + e.getMessage()).build();
        }
    }

    @GET
    @Path("/byCompany")
    public Response getOrdersByCompany(@HeaderParam("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("Valid authentication token required").build();
        }

        String token = authHeader.substring("Bearer ".length());

        try {
            List<CompanyOrderDTO> orders = orderService.getOrdersByCompany(token);

            if (orders.isEmpty()) {
                return Response.status(Response.Status.OK)
                        .entity("No orders found for your company").build();
            }

            return Response.ok(orders).build();
        } catch (SecurityException e) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity(e.getMessage()).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving orders by company: " + e.getMessage()).build();
        }
    }

}