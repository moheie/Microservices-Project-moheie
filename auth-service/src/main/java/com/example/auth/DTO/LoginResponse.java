package com.example.auth.DTO;

import com.example.auth.model.Role;

public class LoginResponse {
    private String token;
    private Long id;
    private String username;
    private String email;
    private String companyName;
    private Role role;

    public LoginResponse(String token, Long id, String username, String email, String companyName, Role role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.companyName = companyName;
        this.role = role;
    }

    public String getToken() {
        return token;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getCompanyName() {
        return companyName;
    }

    public Role getRole() {
        return role;
    }
} 