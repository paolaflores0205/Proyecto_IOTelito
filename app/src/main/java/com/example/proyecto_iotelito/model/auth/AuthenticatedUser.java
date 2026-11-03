package com.example.proyecto_iotelito.model.auth;

public class AuthenticatedUser {
    private final String email;
    private final String displayName;
    private final UserRole role;
    private final String uid;
    private final Integer hotelId;

    public AuthenticatedUser(String email, String displayName, UserRole role) {
        this(email, displayName, role, null);
    }

    public AuthenticatedUser(String email, String displayName, UserRole role, String uid) {
        this(email, displayName, role, uid, null);
    }

    public AuthenticatedUser(String email, String displayName, UserRole role, String uid, Integer hotelId) {
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.uid = uid;
        this.hotelId = hotelId;
    }

    public String getUid() { return uid; }
    public Integer getHotelId() { return hotelId; }

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
