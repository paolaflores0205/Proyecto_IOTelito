package com.example.proyecto_iotelito.data;

import android.content.Context;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.example.proyecto_iotelito.data.taxista.TaxistaAuthRepository;

public final class SessionRepository {
    private static AuthenticatedUser current;

    private SessionRepository() { }

    public static synchronized AuthenticatedUser obtener(Context context) {
        if (current != null && current.getRole() != UserRole.CLIENT) return current;
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        if (firebaseUser == null) {
            current = null;
            return null;
        }
        if (current == null || !firebaseUser.getEmail().equals(current.getEmail())) {
            String email = firebaseUser.getEmail();
            String name = firebaseUser.getDisplayName();
            AuthenticatedUser taxista = TaxistaAuthRepository.restaurar(context, firebaseUser);
            current = taxista != null ? taxista : new AuthenticatedUser(email,
                    name == null || name.isEmpty() ? email : name, UserRole.CLIENT);
        }
        return current;
    }

    public static synchronized void guardar(Context context, AuthenticatedUser user) {
        current = user;
        TaxistaAuthRepository.recordar(context, user);
    }

    public static synchronized void cerrar(Context context) {
        current = null;
        TaxistaAuthRepository.olvidar(context);
        FirebaseAuth.getInstance().signOut();
        context.getSharedPreferences("sesion_cliente", Context.MODE_PRIVATE).edit().clear().apply();
    }
}
