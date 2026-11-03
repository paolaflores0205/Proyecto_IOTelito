package com.example.proyecto_iotelito.data.taxista;

import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;
import java.util.Map;

/** Convierte el perfil remoto del usuario autenticado en el rol de navegación. */
public final class TaxistaSessionProfile {
    private TaxistaSessionProfile() { }

    public static final class TaxistaInactivoException extends IllegalStateException { }

    public static AuthenticatedUser resolver(String email, String displayName, Map<String, Object> profile) {
        UserRole role = UserRole.CLIENT;
        if (profile != null) {
            if (!Boolean.TRUE.equals(profile.get("activo"))) {
                throw new TaxistaInactivoException();
            }
            role = UserRole.TAXISTA;
            Object nombre = profile.get("nombre");
            if (nombre instanceof String) displayName = ((String) nombre).trim();
        }
        return new AuthenticatedUser(email,
                displayName == null || displayName.isEmpty() ? email : displayName, role);
    }
}
