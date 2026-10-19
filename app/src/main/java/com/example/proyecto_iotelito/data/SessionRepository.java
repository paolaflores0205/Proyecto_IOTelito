package com.example.proyecto_iotelito.data;

import android.content.Context;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;

public final class SessionRepository {
    private static AuthenticatedUser current;

    private SessionRepository() { }

    public static synchronized AuthenticatedUser obtener(Context context) {
        return current;
    }

    public static synchronized void guardar(Context context, AuthenticatedUser user) {
        cerrar(context);
        current = user;
    }

    public static synchronized void cerrar(Context context) {
        current = null;
        context.getSharedPreferences("sesion_cliente", Context.MODE_PRIVATE).edit().clear().apply();
    }
}
