package com.example.auth.utils;

import com.example.auth.model.Role;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timeout;
import jakarta.ejb.TimerService;
import jakarta.ejb.TransactionManagement;
import jakarta.ejb.TransactionManagementType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.UserTransaction;

@Singleton
@Startup
@TransactionManagement(TransactionManagementType.BEAN)
public class DatabaseInitializer {

    @PersistenceContext
    private EntityManager entityManager;

    @Resource
    private UserTransaction tx;

    @Resource
    private TimerService timerService;

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "admin123";
    private static final String ADMIN_EMAIL = "admin@fooddelivery.com";

    @PostConstruct
    public void init() {
        // Create a timer that will trigger after schema generation is complete
        timerService.createSingleActionTimer(5000, new jakarta.ejb.TimerConfig());
    }

    @Timeout
    public void initializeAdmin() {
        try {
            tx.begin();

            // Check if admin already exists
            Long adminCount = (Long) entityManager.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE u.username = :username")
                    .setParameter("username", ADMIN_USERNAME)
                    .getSingleResult();

            if (adminCount == 0) {
                // Admin doesn't exist, create it
                String encryptedPassword = Security.encrypt(ADMIN_PASSWORD);

                // Create the admin user using JPA entity
                entityManager.createNativeQuery(
                                "INSERT INTO users (username, password, email, role) VALUES (?, ?, ?, ?)")
                        .setParameter(1, ADMIN_USERNAME)
                        .setParameter(2, encryptedPassword)
                        .setParameter(3, ADMIN_EMAIL)
                        .setParameter(4, Role.ADMIN.toString())
                        .executeUpdate();

                System.out.println("Admin user created successfully");
            } else {
                System.out.println("Admin user already exists");
            }

            tx.commit();
        } catch (Exception e) {
            try {
                tx.rollback();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            System.err.println("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}