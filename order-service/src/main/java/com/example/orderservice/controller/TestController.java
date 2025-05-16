package com.example.orderservice.controller;

import com.example.orderservice.messaging.NotificationSender;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestController {

    @Inject
    private NotificationSender notificationSender;

    @POST
    @Path("/payment-failure")
    public Response testPaymentFailure(
            @QueryParam("orderId") Long orderId,
            @QueryParam("message") String message) {
        try {
            // Send a test payment failure notification
            notificationSender.sendPaymentFailure(
                orderId, 
                message != null ? message : "Test payment failure notification"
            );
            return Response.ok("Payment failure notification sent").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error sending notification: " + e.getMessage()).build();
        }
    }
}
