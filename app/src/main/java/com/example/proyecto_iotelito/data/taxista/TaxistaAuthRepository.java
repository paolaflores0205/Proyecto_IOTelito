package com.example.proyecto_iotelito.data.taxista;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

/** Consulta del perfil taxista y restauración de su rol asociado al UID. */
public final class TaxistaAuthRepository {
    private static final String PREFS = "taxista_firebase_session";

    private TaxistaAuthRepository() { }

    public static Task<AuthenticatedUser> resolver() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Tasks.forException(new IllegalStateException("Sesión requerida"));
        return FirebaseFirestore.getInstance().collection("taxistas").document(user.getUid())
                .get(Source.SERVER).continueWith(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    FirebaseUser actual = FirebaseAuth.getInstance().getCurrentUser();
                    if (actual == null || !actual.getUid().equals(user.getUid())) {
                        throw new IllegalStateException("La sesión cambió");
                    }
                    DocumentSnapshot profile = task.getResult();
                    return TaxistaSessionProfile.resolver(user.getEmail(), user.getDisplayName(),
                            profile.exists() ? profile.getData() : null);
                });
    }

    public static AuthenticatedUser restaurar(Context context, FirebaseUser user) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!user.getUid().equals(prefs.getString("uid", null))) return null;
        return new AuthenticatedUser(user.getEmail(),
                prefs.getString("nombre", user.getEmail()), UserRole.TAXISTA);
    }

    public static void recordar(Context context, AuthenticatedUser user) {
        FirebaseUser firebase = FirebaseAuth.getInstance().getCurrentUser();
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear();
        if (user.getRole() == UserRole.TAXISTA && firebase != null
                && user.getEmail().equalsIgnoreCase(firebase.getEmail())) {
            editor.putString("uid", firebase.getUid()).putString("nombre", user.getDisplayName());
        }
        editor.apply();
    }

    public static void olvidar(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
