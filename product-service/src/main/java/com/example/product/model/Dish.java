package com.example.product.model;


import jakarta.persistence.*;

@Entity
@Table(name = "dish")
@NamedQueries(
        {
                @NamedQuery(name = "Dish.findByName",
                        query = "SELECT d FROM Dish d WHERE d.name = :name"),
                @NamedQuery(name = "Dish.findByCompanyName",
                        query = "SELECT d FROM Dish d WHERE d.companyName = :companyName"),
                @NamedQuery(name = "Dish.findByPrice",
                        query = "SELECT d FROM Dish d WHERE d.price <= :price")
        }
)
public class Dish {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name",nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private double price;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false)
    private int stockCount;

    public Dish() {
    }

    public Dish(String name, String description, double price, String companyName, int stockCount) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.companyName = companyName;
        this.stockCount = stockCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public int getStockCount() {
        return stockCount;
    }

    public void setStockCount(int stockCount) {
        this.stockCount = stockCount;
    }
}