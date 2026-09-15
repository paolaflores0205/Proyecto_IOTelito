package com.example.proyecto_iotelito.model.auth;

public class AuthenticatedUser {
    private final String email;
    private final String displayName;
    private final UserRole role;

    public AuthenticatedUser(String email, String displayName, UserRole role) {
        this.email = email;
        this.displayName = displayName;
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }
}
