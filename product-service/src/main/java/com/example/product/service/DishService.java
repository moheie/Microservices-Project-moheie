package com.example.product.service;

import com.example.product.model.Dish;
import com.example.product.utils.Jwt;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.core.Response;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.Map;

@Stateless
public class DishService {
    @PersistenceContext(unitName = "product-service")
    private EntityManager entityManager;

    @Inject
    private NotificationService notificationService;

    private Long getSellerIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring("Bearer ".length());
        }
        return Jwt.getUserId(token);
    }

    public Response createDish(String name, String description, double price, String companyName, int stockCount, String token) {
        try {
            Long sellerId = getSellerIdFromToken(token);
            Dish dish = new Dish(name, description, price, companyName, stockCount);
            entityManager.persist(dish);

            // Log the new dish creation
            notificationService.sendLogMessage("Dish","Info",
                    "New dish created: " + name + " by " + companyName);

            // Check if initial stock is low
            if (stockCount < 10) {
                notificationService.sendStockNotification(
                        dish.getId(),
                        dish.getName(),
                        stockCount,
                        companyName,
                        sellerId
                );
                notificationService.sendLogMessage("Dish","Warning",
                        "Low initial stock for new dish " + dish.getName() + " (" + companyName + "): " + stockCount);
            }

            return Response.ok("Dish created").build();
        } catch (Exception e) {
            notificationService.sendLogMessage("Dish", "Error", "Failed to create dish: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error creating dish: " + e.getMessage()).build();
        }
    }

    public Response getDishesByCompanyName(String companyName) {
        List<Dish> dishes = entityManager.createNamedQuery("Dish.findByCompanyName", Dish.class)
                .setParameter("companyName", companyName)
                .getResultList();
        return Response.ok(dishes).build();
    }

    public Response updateDish(Long dishId, String name, String description, Double price, Integer stockCount, String companyName, String token) {
        try {
            Long sellerId = getSellerIdFromToken(token);
            Dish dish = entityManager.find(Dish.class, dishId);
            if (dish == null || !dish.getCompanyName().equals(companyName)) {
                return Response.status(Response.Status.NOT_FOUND).entity("Dish not found or unauthorized").build();
            }

            if (name != null && !name.trim().isEmpty()) {
                dish.setName(name);
            }
            if (description != null) {
                dish.setDescription(description);
            }
            if (price != null && price > 0) {
                dish.setPrice(price);
            }

            // Check for stock count changes and send notifications if needed
            if (stockCount != null && stockCount >= 0) {
                int oldStockCount = dish.getStockCount();
                dish.setStockCount(stockCount);

                // Check if stock is low (below 10) and notify sellers
                if (stockCount < 10) {
                    notificationService.sendStockNotification(
                        dishId,
                        dish.getName(),
                        stockCount,
                        companyName,
                        sellerId
                    );

                    // Log low stock event
                    notificationService.sendLogMessage("Dish","Warning",
                            "Low stock for dish " + dish.getName() + " (" + companyName + "): " + stockCount + " remaining");
                }

                // Log if stock was increased
                if (stockCount > oldStockCount) {
                    notificationService.sendLogMessage("Dish","Info",
                            "Stock increased for dish " + dish.getName() + " (" + companyName + "): " +
                                    oldStockCount + " → " + stockCount);
                }
            }

            entityManager.merge(dish);
            return Response.ok("Dish updated successfully").build();
        } catch (Exception e) {
            notificationService.sendLogMessage("Dish", "Error", "Failed to update dish: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error updating dish: " + e.getMessage()).build();
        }
    }

    public Response deleteDish(Long dishId, String companyName) {
        Dish dish = entityManager.find(Dish.class, dishId);
        if (dish == null || !dish.getCompanyName().equals(companyName)) {
            return Response.status(Response.Status.NOT_FOUND).entity("Dish not found or unauthorized").build();
        }
        entityManager.remove(dish);
        return Response.ok("Dish deleted successfully").build();
    }

    public Response getAllDishes() {
        List<Dish> dishes = entityManager.createNamedQuery("Dish.findAll", Dish.class).getResultList();
        return Response.ok(dishes).build();
    }

    //TODO : implement this
    public Response getSoldDishesByCompanyName(String companyName) {
        try {


            return Response.ok().build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving sold dishes: " + e.getMessage()).build();
        }
    }

    @Transactional
    public void decreaseStock(Map<Long, Long> productCounts) {
        System.out.println("\u001B[32m === STOCK DECREASE OPERATION STARTING === \u001B[0m");

        try {
            for (Map.Entry<Long, Long> entry : productCounts.entrySet()) {
                Long productId = entry.getKey();
                Long quantity = entry.getValue();

                Dish dish = entityManager.find(Dish.class, productId);
                if (dish != null) {
                    int oldStock = dish.getStockCount();
                    int newStock = oldStock - quantity.intValue();
                    dish.setStockCount(newStock);

                    entityManager.merge(dish);
                    System.out.println("\u001B[32m Product " + productId + " (" + dish.getName() +
                            "): Stock DECREASED from " + oldStock +
                            " to " + newStock + "\u001B[0m");

                    // Check if stock is low after decrease
                    if (newStock < 10) {
                        // For stock decrease during orders, we need to make an HTTP call to auth service
                        // to get the seller ID. For now, we'll just use the companyName in the message
                        notificationService.sendStockNotification(
                            productId,
                            dish.getName(),
                            newStock,
                            dish.getCompanyName(),
                            null  // The notification service will need to look up the seller ID
                        );
                    }

                    notificationService.sendLogMessage("Dish","Info",
                            "Stock decreased for product " + dish.getName() +
                                    " (ID: " + productId + "): " +
                                    oldStock + " → " + newStock);
                }
            }
            System.out.println("\u001B[32m === STOCK DECREASE COMMITTED SUCCESSFULLY === \u001B[0m");
        } catch (Exception e) {
            System.out.println("\u001B[31m === STOCK DECREASE FAILED: " + e.getMessage() + " === \u001B[0m");
            e.printStackTrace();
            throw e; // Rethrow to trigger transaction rollback
        }
    }
}