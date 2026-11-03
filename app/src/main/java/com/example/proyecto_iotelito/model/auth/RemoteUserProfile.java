package com.example.proyecto_iotelito.model.auth;

import java.util.Map;

/** Contrato de identidad de Firestore. No depende de Android para poder probarlo. */
public final class RemoteUserProfile {
    public final String uid, name, email;
    public final UserRole role;
    public final boolean active;
    public final Integer hotelId;

    private RemoteUserProfile(String uid, String name, String email, UserRole role, boolean active,
                              Integer hotelId) {
        this.uid = uid; this.name = name; this.email = email; this.role = role; this.active = active;
        this.hotelId = hotelId;
    }

    public static RemoteUserProfile parse(String documentId, Map<String, Object> data) {
        if (documentId == null || documentId.isEmpty() || data == null
                || !documentId.equals(data.get("uid")) || !(data.get("active") instanceof Boolean)) {
            throw new IllegalArgumentException("Perfil incompleto: revisa uid y active en users.");
        }
        UserRole role;
        try { role = UserRole.valueOf(String.valueOf(data.get("role"))); }
        catch (IllegalArgumentException error) { throw new IllegalArgumentException("El perfil no tiene un rol válido."); }
        String name = text(data.get("name")), email = text(data.get("email"));
        if (name.isEmpty() || email.isEmpty()) throw new IllegalArgumentException("El perfil no tiene nombre o correo.");
        Object rawHotelId = data.get("hotelId");
        Integer hotelId = null;
        if (rawHotelId != null) {
            if (!(rawHotelId instanceof Number)) throw new IllegalArgumentException("El hotel asignado no es válido.");
            double number = ((Number) rawHotelId).doubleValue();
            if (!Double.isFinite(number) || number != Math.rint(number) || number <= 0
                    || number > Integer.MAX_VALUE) throw new IllegalArgumentException("El hotel asignado no es válido.");
            hotelId = (int) number;
        }
        return new RemoteUserProfile(documentId, name, email, role, (Boolean) data.get("active"), hotelId);
    }

    public AuthenticatedUser authenticated() {
        if (!active) throw new IllegalArgumentException("Tu cuenta está inactiva. Comunícate con el administrador.");
        if (role == UserRole.HOTEL_ADMIN && hotelId == null)
            throw new IllegalArgumentException("Tu cuenta todavía no tiene un hotel asignado.");
        return new AuthenticatedUser(email, name, role, uid, hotelId);
    }

    public String label() {
        switch (role) {
            case CLIENT: return "Cliente";
            case HOTEL_ADMIN: return "Administrador";
            case TAXISTA: return "Taxista";
            case SUPERADMIN: return "Superadmin";
            default: throw new IllegalStateException("Rol desconocido");
        }
    }

    public static String text(Object value) { return value instanceof String ? ((String) value).trim() : ""; }
}
