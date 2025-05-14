package com.example.notificationservice.repository;

import com.example.notificationservice.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {
    
    // Find notifications by user type
    List<Notification> findByUserType(String userType);
    
    // Find notifications by user ID
    List<Notification> findByUserId(Long userId);
    
    // Find unread notifications by user type
    List<Notification> findByUserTypeAndReadFalse(String userType);
    
    // Find unread notifications by user ID
    List<Notification> findByUserIdAndReadFalse(Long userId);
    
    // Find notifications by type (e.g., payment, order, error)
    List<Notification> findByType(String type);
}
