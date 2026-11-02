package com.example.proyecto_iotelito.model.auth;

public class AuthenticatedUser {
    private final String email;
    private final String displayName;
    private final UserRole role;
    private final String uid;

    public AuthenticatedUser(String email, String displayName, UserRole role) {
        this(email, displayName, role, null);
    }

    public AuthenticatedUser(String email, String displayName, UserRole role, String uid) {
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.uid = uid;
    }

    public String getUid() { return uid; }

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
