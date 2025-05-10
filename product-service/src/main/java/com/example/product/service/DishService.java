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


}