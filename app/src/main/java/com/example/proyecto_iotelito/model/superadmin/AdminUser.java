package com.example.proyecto_iotelito.model.superadmin;

public class AdminUser {
    public final int id;
    public final String name;
    public final String email;
    public final String document;
    public final String phone;
    public final String role;
    public final String registeredAt;
    public final String lastAccess;
    public boolean active;

    public AdminUser(int id, String name, String email, String document, String phone,
                     String role, String registeredAt, String lastAccess, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.document = document;
        this.phone = phone;
        this.role = role;
        this.registeredAt = registeredAt;
        this.lastAccess = lastAccess;
        this.active = active;
    }
}
