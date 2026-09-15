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
    public String tipoDocumento;
    public String numeroDocumento;
    public String fechaNacimiento;
    public String domicilio;

    public UserProfile(String name, String email, String phone) {
        this(name, email, phone, "DNI", "45678912", "15/03/1990", "Av. Larco 456, Miraflores, Lima");
    }

    public UserProfile(String name, String email, String phone, String tipoDocumento,
                       String numeroDocumento, String fechaNacimiento, String domicilio) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
        this.fechaNacimiento = fechaNacimiento;
        this.domicilio = domicilio;
    }

    public String firstName() {
        int spaceIndex = name.indexOf(' ');
        return spaceIndex > 0 ? name.substring(0, spaceIndex) : name;
    }

    public String initials() {
        if (name == null || name.trim().isEmpty()) return "CB";
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
