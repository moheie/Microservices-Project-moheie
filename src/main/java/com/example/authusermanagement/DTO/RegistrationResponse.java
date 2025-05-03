package com.example.authusermanagement.DTO;

public class RegistrationResponse {
    private String message;
    private String password;

    public RegistrationResponse(String message, String password) {
        this.message = message;
        this.password = password;
    }

    public String getMessage() {
        return message;
    }

    public String getPassword() {
        return password;
    }
}
