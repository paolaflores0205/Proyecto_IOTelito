package com.example.proyecto_iotelito.data.taxista;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Consulta del perfil taxista y restauración de su rol asociado al UID. */
public final class TaxistaAuthRepository {
    private static final java.util.concurrent.ExecutorService IO =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final String PREFS = "taxista_firebase_session";

    private TaxistaAuthRepository() {}

    public static Task<AuthenticatedUser> resolver() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Tasks.forException(new IllegalStateException("Sesión requerida"));
        return Tasks.call(
                IO,
                () -> {
                    org.json.JSONObject p =
                            com.example.proyecto_iotelito.data.taxiapi.TaxiApis.forDriver(user)
                                    .miPerfilTaxista();
                    if (!p.optBoolean("activo")
                            || !"APROBADO".equals(p.optString("estadoSolicitud")))
                        throw new com.example.proyecto_iotelito.data.taxiapi.TaxiApiException(
                                403,
                                com.example.proyecto_iotelito.data.taxiapi.TaxiApiException.Codigo
                                        .TAXISTA_NO_HABILITADO,
                                "Tu cuenta de taxista no está habilitada.");
                    return new AuthenticatedUser(
                            user.getEmail(),
                            p.optString("nombres") + " " + p.optString("apellidos"),
                            UserRole.TAXISTA,
                            user.getUid());
                });
    }

    public static AuthenticatedUser restaurar(Context context, FirebaseUser user) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        if (!user.getUid().equals(prefs.getString("uid", null))) return null;
        return new AuthenticatedUser(
                user.getEmail(), prefs.getString("nombre", user.getEmail()), UserRole.TAXISTA);
    }

    public static void recordar(Context context, AuthenticatedUser user) {
        FirebaseUser firebase = FirebaseAuth.getInstance().getCurrentUser();
        SharedPreferences.Editor editor =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear();
        if (user.getRole() == UserRole.TAXISTA
                && firebase != null
                && user.getEmail().equalsIgnoreCase(firebase.getEmail())) {
            editor.putString("uid", firebase.getUid()).putString("nombre", user.getDisplayName());
        }
        editor.apply();
    }

    public static void olvidar(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
