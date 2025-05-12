package com.example.product.service;

import com.example.product.model.Dish;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Stateless
public class DishService {
    @PersistenceContext(unitName = "product-service")
    EntityManager entityManager;

    public Response createDish(String name, String description, double price, String companyName, int stockCount) {
        Dish dish = new Dish(name, description, price, companyName, stockCount);
        entityManager.persist(dish);
        return Response.ok("Dish created").build();
    }

    //probably wont need this IDs are mainly used for database
    public Response getDishByID(Long id) {
        Dish dish = entityManager.find(Dish.class, id);
        if (dish == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Dish not found").build();
        }
        return Response.ok(dish).build();
    }

    public Response getDishByName(String name){
        Dish dish = entityManager.createNamedQuery("Dish.findByName", Dish.class)
                .setParameter("name", name)
                .getSingleResult();
        if (dish == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Dish not found").build();
        }
        return Response.ok(dish).build();
    }

    public Response getDishesByCompanyName(String companyName){
        List<Dish> dishes = entityManager.createNamedQuery("Dish.findByCompanyName", Dish.class)
                .setParameter("companyName", companyName)
                .getResultList();
        return Response.ok(dishes).build();
    }

    public Response updateDish(Long dishId, String name, String description, Double price, Integer stockCount, String companyName) {
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
        if (stockCount != null && stockCount >= 0) {
            dish.setStockCount(stockCount);
        }

        entityManager.merge(dish);
        return Response.ok("Dish updated successfully").build();
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
        }
        catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity("Error retrieving sold dishes: " + e.getMessage()).build();
        }
    }

}