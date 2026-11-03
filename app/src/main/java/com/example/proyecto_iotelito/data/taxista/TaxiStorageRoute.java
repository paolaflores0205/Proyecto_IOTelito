package com.example.proyecto_iotelito.data.taxista;

import com.example.proyecto_iotelito.model.auth.UserRole;

/** Mantiene los pedidos locales previos sin exponer los de otra cuenta Firebase. */
public final class TaxiStorageRoute {
    private TaxiStorageRoute() { }

    public static boolean usarLocal(boolean firebase, UserRole role, String email, String propietario) {
        return !firebase || (role == UserRole.CLIENT && email != null && propietario != null
                && email.equalsIgnoreCase(propietario));
    }
}
