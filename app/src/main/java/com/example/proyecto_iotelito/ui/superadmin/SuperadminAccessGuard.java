package com.example.proyecto_iotelito.ui.superadmin;

import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.Toast;
import com.example.proyecto_iotelito.data.FirebaseProfileRepository;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.example.proyecto_iotelito.ui.auth.LoginActivity;

/** Protege también actividades abiertas desde notificaciones o intents internos. */
public final class SuperadminAccessGuard {
    private SuperadminAccessGuard() { }
    public static void verify(Activity activity) {
        if (!activity.getClass().getName().startsWith("com.example.proyecto_iotelito.ui.superadmin.")) return;
        View content = activity.findViewById(android.R.id.content);
        if (content != null) content.setVisibility(View.INVISIBLE);
        AuthenticatedUser session = SessionRepository.obtener(activity);
        if (session == null || session.getUid() == null || session.getRole() != UserRole.SUPERADMIN) {
            redirect(activity, "Inicia sesión con una cuenta superadmin."); return;
        }
        FirebaseProfileRepository.loadCurrent((user, error) -> {
            if (activity.isFinishing() || activity.isDestroyed()) return;
            if (user == null || user.getRole() != UserRole.SUPERADMIN) {
                redirect(activity, error == null ? "Esta cuenta no tiene acceso a superadmin." : error);
            } else {
                SessionRepository.guardar(activity, user);
                if (content != null) content.setVisibility(View.VISIBLE);
            }
        });
    }
    private static void redirect(Activity activity, String message) {
        SessionRepository.cerrar(activity);
        Toast.makeText(activity, message, Toast.LENGTH_LONG).show();
        activity.startActivity(new Intent(activity, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        activity.finish();
    }
}
