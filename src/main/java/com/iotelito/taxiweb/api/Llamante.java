package com.iotelito.taxiweb.api;

/**
 * Quien llama a la API: su cuenta de Firebase y su perfil users/{uid} de Firestore.
 * {@code token} se reenvía a Firestore para leer con sus mismos permisos.
 */
public record Llamante(String uid, String correo, String rol, Long hotelId, String nombre, String token) {

    public static final String CLIENTE = "CLIENT";
    public static final String TAXISTA = "TAXISTA";
    public static final String ADMIN_HOTEL = "HOTEL_ADMIN";
    public static final String SUPERADMIN = "SUPERADMIN";

    public boolean es(String otroRol) {
        return otroRol.equals(rol);
    }
}
