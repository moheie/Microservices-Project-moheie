package com.example.product.controller;

import com.example.product.service.NotificationService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TestController {

    @Inject
    private NotificationService notificationService;

    @POST
    @Path("/system-error")
    public Response testSystemError(
            @QueryParam("serviceName") String serviceName,
            @QueryParam("severity") String severity,
            @QueryParam("message") String message) {
        try {
            // Send a test error log notification
            notificationService.sendLogMessage(
                serviceName != null ? serviceName : "ProductService",
                severity != null ? severity : "Error",
                message != null ? message : "Test system error notification"
            );
            return Response.ok("System error notification sent").build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error sending notification: " + e.getMessage()).build();
        }
    }
}
