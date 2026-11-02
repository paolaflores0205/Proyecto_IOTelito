package com.example.proyecto_iotelito.data;

import android.content.Context;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public final class SessionRepository {
    private static AuthenticatedUser current;
    private static com.google.firebase.firestore.ListenerRegistration profileListener;
    private static String watchedUid;

    private SessionRepository() { }

    public static synchronized AuthenticatedUser obtener(Context context) {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        // Compatibilidad con fixtures locales antiguos; el login público ya no crea estas sesiones.
        if (current != null && current.getUid() == null
                && (current.getRole() == UserRole.HOTEL_ADMIN || current.getRole() == UserRole.TAXISTA)) return current;
        if (firebaseUser == null) {
            current = null;
            return null;
        }
        // No inventar rol CLIENT al restaurar Firebase Auth: primero leer users en servidor.
        if (current == null || !firebaseUser.getUid().equals(current.getUid())) return null;
        return current;
    }

    public static synchronized void guardar(Context context, AuthenticatedUser user) {
        if (user.getRole() == UserRole.SUPERADMIN && user.getUid() == null)
            throw new IllegalArgumentException("Superadmin requiere una identidad Firebase verificada.");
        current = user;
        if (user.getUid() != null) watchProfile(context.getApplicationContext(), user);
    }

    private static void watchProfile(Context context, AuthenticatedUser user) {
        if (user.getUid().equals(watchedUid) && profileListener != null) return;
        stopWatching();
        watchedUid = user.getUid();
        String uid = watchedUid;
        profileListener = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE, (doc, error) -> {
                    if (!uid.equals(watchedUid)) return;
                    if (error == null && doc != null && doc.getMetadata().isFromCache()) return;
                    try {
                        if (error != null || doc == null || !doc.exists()) throw new IllegalArgumentException();
                        AuthenticatedUser verified = com.example.proyecto_iotelito.model.auth.RemoteUserProfile
                                .parse(uid, doc.getData()).authenticated();
                        if (verified.getRole() != user.getRole()) throw new IllegalArgumentException();
                        current = verified;
                    } catch (IllegalArgumentException invalid) {
                        cerrar(context);
                        android.widget.Toast.makeText(context, "La cuenta cambió o fue desactivada. Inicia sesión nuevamente.",
                                android.widget.Toast.LENGTH_LONG).show();
                        context.startActivity(new android.content.Intent(context,
                                com.example.proyecto_iotelito.ui.auth.LoginActivity.class)
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK));
                    }
                });
    }

    private static void stopWatching() {
        watchedUid = null;
        if (profileListener != null) profileListener.remove();
        profileListener = null;
    }

    public static synchronized void cerrar(Context context) {
        stopWatching();
        current = null;
        FirebaseAuth.getInstance().signOut();
        context.getSharedPreferences("sesion_cliente", Context.MODE_PRIVATE).edit().clear().apply();
    }
}
