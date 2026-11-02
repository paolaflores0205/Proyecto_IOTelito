package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.RemoteUserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import java.util.Map;

/** Firebase Auth autentica; el documento users/{uid} resuelve rol y habilitación. */
public final class FirebaseProfileRepository {
    public interface Callback { void complete(AuthenticatedUser user, String error); }
    private FirebaseProfileRepository() { }

    public static void loadCurrent(Callback callback) {
        FirebaseUser account = FirebaseAuth.getInstance().getCurrentUser();
        if (account == null) { callback.complete(null, "Inicia sesión nuevamente."); return; }
        String uid = account.getUid();
        FirebaseFirestore.getInstance().collection("users").document(uid).get(Source.SERVER)
                .addOnSuccessListener(doc -> {
                    FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
                    if (current == null || !uid.equals(current.getUid())) {
                        callback.complete(null, "La sesión cambió. Vuelve a ingresar."); return;
                    }
                    if (!doc.exists()) {
                        callback.complete(null, "La cuenta no tiene perfil en users. Solicita su configuración al administrador.");
                        return;
                    }
                    try { callback.complete(RemoteUserProfile.parse(uid, doc.getData()).authenticated(), null); }
                    catch (IllegalArgumentException error) { callback.complete(null, error.getMessage()); }
                })
                .addOnFailureListener(error -> callback.complete(null,
                        "No se pudo verificar tu perfil en el servidor. Revisa la conexión y las reglas de Firestore."));
    }

    public static com.google.android.gms.tasks.Task<Void> createClient(String uid, Map<String, Object> fields) {
        return FirebaseFirestore.getInstance().collection("users").document(uid).set(fields);
    }
}
