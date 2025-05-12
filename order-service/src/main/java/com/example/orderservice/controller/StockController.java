//package com.example.orderservice.controller;
//
//import com.example.orderservice.service.StockService;
//import jakarta.ejb.EJB;
//import jakarta.ws.rs.*;
//import jakarta.ws.rs.core.MediaType;
//import jakarta.ws.rs.core.Response;
//
//@Path("/stock")
//@Produces(MediaType.APPLICATION_JSON)
//@Consumes(MediaType.APPLICATION_JSON)
//public class StockController {
//    @EJB
//    private StockService stockService;
//
//    @POST
//    @Path("/check")
//    public Response checkStock(@QueryParam("orderId") Long orderId, @QueryParam("productIds") String productIds) {
//        if (orderId == null || productIds == null || productIds.isEmpty()) {
//            return Response.status(Response.Status.BAD_REQUEST)
//                    .entity("Order ID and product IDs are required").build();
//        }
//        //TODO: check if stock is available
//        try {
//            //boolean inStock = stockService.checkStock(productIds.split(","));
//            //return Response.ok("Stock check completed: " + inStock).build();
//        } catch (Exception e) {
//            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
//                    .entity("Error checking stock: " + e.getMessage()).build();
//        }
//    }
//}