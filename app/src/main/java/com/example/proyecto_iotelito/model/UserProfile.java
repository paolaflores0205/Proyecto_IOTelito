package com.example.proyecto_iotelito.model;

/**
 * Perfil del cliente. Mutable a propósito: la pantalla de edición de
 * perfil actualiza esta misma instancia en memoria (sin backend todavía),
 * para que el cambio se refleje de inmediato en el resto de la app.
 */
public class UserProfile {

    public String name;
    public String email;
    public String phone;

    public UserProfile(String name, String email, String phone) {
        this.name = name;
        this.email = email;
        this.phone = phone;
    }

    public String firstName() {
        int spaceIndex = name.indexOf(' ');
        return spaceIndex > 0 ? name.substring(0, spaceIndex) : name;
    }

    public String initials() {
        String[] parts = name.trim().split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < parts.length && i < 2; i++) {
            if (!parts[i].isEmpty()) {
                builder.append(Character.toUpperCase(parts[i].charAt(0)));
            }
        }
        return builder.toString();
    }
}
