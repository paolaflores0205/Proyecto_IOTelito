package com.example.proyecto_iotelito.data.taxista;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.TransactionOptions;
import java.util.HashMap;
import java.util.Map;

/** Una suscripción por ViewModel, compartida por Inicio y Perfil del taxista. */
public final class DisponibilidadRepository implements AutoCloseable {
    private static final Map<String, DisponibilidadRepository> INSTANCIAS = new HashMap<>();
    private final String clave;
    private int usuarios;
    private final Context context;
    private final String cuenta;
    private final String uid;
    private final DocumentReference perfil;
    private final MutableLiveData<DisponibilidadEstado> estado;
    private ListenerRegistration listener;
    private boolean cerrado;

    public static synchronized DisponibilidadRepository adquirir(Context context, String cuenta) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String clave = cuenta + ":" + (user == null ? "local" : user.getUid());
        DisponibilidadRepository repository = INSTANCIAS.get(clave);
        if (repository == null) {
            repository = new DisponibilidadRepository(context, cuenta, clave);
            INSTANCIAS.put(clave, repository);
        }
        repository.usuarios++;
        return repository;
    }

    private DisponibilidadRepository(Context context, String cuenta, String clave) {
        this.clave = clave;
        this.context = context.getApplicationContext();
        this.cuenta = cuenta;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        boolean remoto = user != null && cuenta.equalsIgnoreCase(user.getEmail())
                && TaxistaAuthRepository.restaurar(context, user) != null;
        uid = remoto ? user.getUid() : null;
        perfil = remoto ? FirebaseFirestore.getInstance().collection("taxistas").document(uid) : null;
        estado = new MutableLiveData<>(remoto ? DisponibilidadEstado.inicial()
                : DisponibilidadEstado.local(ConductorPreferencias.disponible(context, cuenta)));
        if (remoto) {
            reflejar(estado.getValue());
            listener = perfil.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                if (cerrado) return;
                if (!mismaSesion()) { publicar(estado.getValue().desconectar(DisponibilidadEstado.Problema.SESION)); return; }
                if (error != null) { publicar(estado.getValue().desconectar(problema(error))); return; }
                if (snapshot == null || snapshot.getMetadata().isFromCache()) {
                    publicar(estado.getValue().desconectar(DisponibilidadEstado.Problema.CONEXION));
                    return;
                }
                if (snapshot.getMetadata().hasPendingWrites()) return;
                publicar(estado.getValue().servidor(snapshot.get("activo"), snapshot.get("disponible")));
            });
        }
    }

    public LiveData<DisponibilidadEstado> estado() { return estado; }

    public void cambiar(boolean disponible) {
        DisponibilidadEstado anterior = estado.getValue();
        if (cerrado || !anterior.editable() || anterior.disponible == disponible) return;
        if (perfil == null) { publicar(DisponibilidadEstado.local(disponible)); return; }
        if (!mismaSesion()) { publicar(anterior.fallar(DisponibilidadEstado.Problema.SESION)); return; }
        publicar(anterior.iniciarCambio());
        // La transacción falla sin conexión, sin dejar una escritura diferida para otra sesión.
        FirebaseFirestore.getInstance().runTransaction(
                new TransactionOptions.Builder().setMaxAttempts(2).build(), transaction -> {
                    if (!mismaSesion()) throw new IllegalStateException("Sesión de taxista requerida");
                    DocumentSnapshot snapshot = transaction.get(perfil);
                    if (!snapshot.exists() || !Boolean.TRUE.equals(snapshot.get("activo"))
                            || !(snapshot.get("disponible") instanceof Boolean)) {
                        throw new PerfilInvalidoException();
                    }
                    transaction.update(perfil, "disponible", disponible);
                    return disponible;
                }).addOnCompleteListener(task -> {
                    if (cerrado) return;
                    if (!mismaSesion()) { publicar(estado.getValue().fallar(DisponibilidadEstado.Problema.SESION)); return; }
                    if (task.isSuccessful()) publicar(estado.getValue().confirmar(disponible));
                    else publicar(estado.getValue().fallar(problema(task.getException())));
                });
    }

    private boolean mismaSesion() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null && uid.equals(user.getUid())
                && TaxistaAuthRepository.restaurar(context, user) != null;
    }

    private static final class PerfilInvalidoException extends RuntimeException { }

    private DisponibilidadEstado.Problema problema(Exception error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof PerfilInvalidoException) return DisponibilidadEstado.Problema.PERFIL;
        }
        if (error instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) error).getCode();
            if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return DisponibilidadEstado.Problema.PERMISOS;
            if (code == FirebaseFirestoreException.Code.UNAVAILABLE
                    || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) return DisponibilidadEstado.Problema.CONEXION;
        }
        return DisponibilidadEstado.Problema.GUARDADO;
    }

    private void publicar(DisponibilidadEstado nuevo) {
        reflejar(nuevo);
        estado.setValue(nuevo);
    }

    private void reflejar(DisponibilidadEstado valor) {
        if (perfil != null) ConductorPreferencias.confirmar(cuenta, valor.editable());
        ConductorPreferencias.get(context, cuenta).edit()
                .putBoolean("disponible", valor.disponible)
                .putBoolean("disponible_confirmada", valor.editable()).apply();
    }

    @Override public void close() {
        synchronized (DisponibilidadRepository.class) {
            if (cerrado || --usuarios > 0) return;
            INSTANCIAS.remove(clave);
        }
        cerrado = true;
        if (listener != null) listener.remove();
        if (perfil != null) {
            ConductorPreferencias.confirmar(cuenta, false);
            ConductorPreferencias.get(context, cuenta).edit()
                    .putBoolean("disponible_confirmada", false).apply();
        }
    }
}
