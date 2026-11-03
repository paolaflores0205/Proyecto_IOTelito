package com.example.proyecto_iotelito.data.superadmin;

import androidx.lifecycle.LiveData;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.superadmin.ActivityLog;
import java.time.LocalDate;
import java.util.*;

/** Lecturas administrativas separadas de la consulta personal del cliente. */
public final class FirebaseAdminReadRepository {
    public static final class State<T> {
        public final List<T> rows; public final String error; public final boolean loading;
        State(List<T> rows, boolean loading, String error) { this.rows = rows; this.loading = loading; this.error = error; }
    }
    private interface Parser<T> { T parse(DocumentSnapshot doc); }
    private <T> LiveData<State<T>> observe(Query query, Parser<T> parser) {
        return new LiveData<State<T>>(new State<>(Collections.emptyList(), true, null)) {
            private ListenerRegistration listener;
            private FirebaseAuth.AuthStateListener auth;
            @Override protected void onActive() {
                auth = session -> {
                    if (listener != null) listener.remove(); listener = null;
                    setValue(new State<>(Collections.emptyList(), true, null));
                    if (session.getCurrentUser() == null) {
                        setValue(new State<>(Collections.emptyList(), false, "Inicia sesión para continuar.")); return;
                    }
                    String uid = session.getCurrentUser().getUid();
                    listener = query.addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                        if (session.getCurrentUser() == null || !uid.equals(session.getCurrentUser().getUid())) return;
                        if (error != null || snapshot == null) {
                            setValue(new State<>(Collections.emptyList(), false, "No se pudo actualizar la información. Revisa tu conexión o vuelve a iniciar sesión.")); return;
                        }
                        if (snapshot.getMetadata().isFromCache()) {
                            setValue(new State<>(Collections.emptyList(), true, null)); return;
                        }
                        List<T> rows = new ArrayList<>(); int invalid = 0;
                        for (DocumentSnapshot doc : snapshot) {
                            try { rows.add(parser.parse(doc)); } catch (RuntimeException malformed) { invalid++; }
                        }
                        setValue(new State<>(rows, false, invalid == 0 ? null : invalid + " registros no se pudieron procesar. Revisa sus datos."));
                    });
                };
                FirebaseAuth.getInstance().addAuthStateListener(auth);
            }
            @Override protected void onInactive() {
                if (listener != null) listener.remove(); listener = null;
                if (auth != null) FirebaseAuth.getInstance().removeAuthStateListener(auth); auth = null;
                setValue(new State<>(Collections.emptyList(), true, null));
            }
        };
    }
    public LiveData<State<Reserva>> reservations() {
        return observe(FirebaseFirestore.getInstance().collection("reservations"), doc -> {
            int id = HotelFirestoreCodec.integer(doc.get("id"));
            if (!String.valueOf(id).equals(doc.getId())) throw new IllegalArgumentException("ID inconsistente");
            double total = ((Number) doc.get("precioTotal")).doubleValue();
            LocalDate start = LocalDate.parse(doc.getString("fechaEntrada")), end = LocalDate.parse(doc.getString("fechaSalida"));
            if (!Double.isFinite(total) || total < 0 || !start.isBefore(end)) throw new IllegalArgumentException("Reserva inválida");
            return new Reserva(id, HotelFirestoreCodec.integer(doc.get("hotelId")), doc.getString("roomName"),
                    doc.getString("roomNumber"), start, end, doc.getString("huespedes"), total,
                    Reserva.Estado.valueOf(doc.getString("estado")), doc.getString("codigo"));
        });
    }
    public LiveData<State<ActivityLog>> logs() {
        return observe(FirebaseFirestore.getInstance().collection("logs").orderBy("createdAt", Query.Direction.DESCENDING).limit(200), doc -> {
            com.google.firebase.Timestamp time = doc.getTimestamp("createdAt");
            String action = doc.getString("action");
            return new ActivityLog(action != null && action.startsWith("USER_") ? "Usuario" : "Sistema",
                    actionTitle(action), "hotels".equals(doc.getString("entityType"))
                    ? "Hotel #" + doc.getString("entityId") : "Gestión de cuentas y acceso", time == null ? "Pendiente" :
                    new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(time.toDate()));
        });
    }
    private static String actionTitle(String action) {
        if (action == null) return "Actividad registrada";
        switch (action) {
            case "HOTEL_CREATED": return "Hotel registrado";
            case "HOTEL_UPDATED": return "Información del hotel actualizada";
            case "HOTEL_ACTIVATED": return "Hotel activado";
            case "HOTEL_DEACTIVATED": return "Hotel desactivado";
            case "HOTEL_IMPORTED": return "Hotel incorporado";
            case "HOTEL_ADMIN_ASSIGNED": return "Administrador asignado";
            case "HOTEL_ADMIN_REMOVED": return "Administrador desvinculado";
            case "USER_ACTIVATED": return "Usuario activado";
            case "USER_DEACTIVATED": return "Usuario desactivado";
            default: return "Actividad registrada";
        }
    }
}
