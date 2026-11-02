package com.example.proyecto_iotelito.data.superadmin;

import androidx.lifecycle.LiveData;
import com.example.proyecto_iotelito.model.auth.RemoteUserProfile;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.example.proyecto_iotelito.model.superadmin.AdminUser;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;

/** Lista remota sin mezclar usuarios de demostración ni inventar IDs enteros para UID. */
public final class FirebaseUsersRepository {
    public static final class State {
        public final List<AdminUser> users;
        public final boolean loading;
        public final String error;
        State(List<AdminUser> users, boolean loading, String error) {
            this.users = Collections.unmodifiableList(new ArrayList<>(users));
            this.loading = loading; this.error = error;
        }
    }
    public LiveData<State> observeUsers() {
        return new LiveData<State>(new State(Collections.emptyList(), true, null)) {
            private ListenerRegistration listener;
            private FirebaseAuth.AuthStateListener authListener;
            @Override protected void onActive() {
                authListener = auth -> {
                    if (!hasActiveObservers()) return;
                    if (listener != null) listener.remove();
                    listener = null;
                    setValue(new State(Collections.emptyList(), true, null));
                    if (auth.getCurrentUser() == null) {
                        setValue(new State(Collections.emptyList(), false, "Inicia sesión con Firebase.")); return;
                    }
                    String uid = auth.getCurrentUser().getUid();
                    listener = FirebaseFirestore.getInstance().collection("users")
                            .addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                                FirebaseUser now = FirebaseAuth.getInstance().getCurrentUser();
                                if (now == null || !uid.equals(now.getUid())) return;
                                if (error != null || snapshot == null) {
                                    setValue(new State(Collections.emptyList(), false,
                                            "No se pudieron cargar los usuarios. Revisa tu rol, conexión y reglas de Firestore.")); return;
                                }
                                if (snapshot.getMetadata().isFromCache()) {
                                    setValue(new State(Collections.emptyList(), false,
                                            "Esperando confirmación del servidor. No se muestran perfiles guardados en caché.")); return;
                                }
                                List<AdminUser> users = new ArrayList<>();
                                int invalid = 0;
                                for (DocumentSnapshot doc : snapshot.getDocuments()) {
                                    try {
                                        RemoteUserProfile p = RemoteUserProfile.parse(doc.getId(), doc.getData());
                                        AdminUser user = new AdminUser(-1, p.name, p.email,
                                                RemoteUserProfile.text(doc.get("documentNumber")),
                                                RemoteUserProfile.text(doc.get("phone")), p.label(),
                                                date(doc.getTimestamp("createdAt")), date(doc.getTimestamp("lastAccessAt")), p.active);
                                        user.uid = p.uid;
                                        users.add(user);
                                    } catch (RuntimeException malformed) { invalid++; }
                                }
                                users.sort(Comparator.comparing(u -> u.name.toLowerCase(Locale.ROOT)));
                                setValue(new State(users, false, invalid == 0 ? null
                                        : "Hay " + invalid + " perfiles incompletos en users. Revisa su UID, nombre, correo, rol y active."));
                            });
                };
                FirebaseAuth.getInstance().addAuthStateListener(authListener);
            }
            @Override protected void onInactive() {
                if (listener != null) listener.remove();
                listener = null;
                if (authListener != null) FirebaseAuth.getInstance().removeAuthStateListener(authListener);
                authListener = null;
                setValue(new State(Collections.emptyList(), true, null));
            }
        };
    }
    private static String date(com.google.firebase.Timestamp timestamp) {
        return timestamp == null ? "Sin registro" : new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(timestamp.toDate());
    }
    public void setActive(String uid, boolean active, String reason, SuperadminRepository.Callback callback) {
        FirebaseUser actor = FirebaseAuth.getInstance().getCurrentUser();
        if (actor == null || uid == null || uid.isEmpty() || (!active && reason.trim().isEmpty())) {
            callback.complete(-1, "Verifica tu sesión, el usuario y el motivo de desactivación."); return;
        }
        String actorUid = actor.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference actorRef = db.collection("users").document(actorUid);
        DocumentReference targetRef = db.collection("users").document(uid);
        db.runTransaction(tx -> {
            DocumentSnapshot actorDoc = tx.get(actorRef), targetDoc = tx.get(targetRef);
            RemoteUserProfile owner = RemoteUserProfile.parse(actorUid, actorDoc.getData());
            if (!owner.active || owner.role != UserRole.SUPERADMIN)
                throw new IllegalArgumentException("Esta operación requiere un superadmin activo.");
            RemoteUserProfile target = RemoteUserProfile.parse(uid, targetDoc.getData());
            if (uid.equals(actorUid) || target.role == UserRole.SUPERADMIN)
                throw new IllegalArgumentException("No puedes cambiar desde aquí el estado de un superadmin.");
            Map<String, Object> update = new HashMap<>();
            update.put("active", active);
            update.put("statusReason", active ? "" : reason.trim());
            update.put("updatedAt", FieldValue.serverTimestamp());
            update.put("updatedByUid", actorUid);
            tx.update(targetRef, update);
            return null;
        }).addOnSuccessListener(unused -> callback.complete(-1, null))
                .addOnFailureListener(error -> callback.complete(-1,
                        "No se cambió el estado. Revisa permisos, conexión y que el usuario no sea superadmin."));
    }
}
