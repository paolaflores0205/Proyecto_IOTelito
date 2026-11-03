package com.example.proyecto_iotelito.data.hoteladmin;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.data.superadmin.HotelFirestoreCodec;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.model.auth.*;
import com.example.proyecto_iotelito.model.hoteladmin.*;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.google.android.gms.tasks.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.function.Consumer;

/** Firestore para el hotel asignado. Las listas remotas no usan datos de Room como reemplazo. */
public final class FirebaseHotelAdminRepository {
    public static final class State<T> {
        public final T data; public final String error; public final boolean loading;
        State(T data, String error, boolean loading) { this.data = data; this.error = error; this.loading = loading; }
    }
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final Context context;
    public final String uid;
    public final int hotelId;
    private final DocumentReference hotel, profile;
    public FirebaseHotelAdminRepository(Context context) {
        this.context = context.getApplicationContext();
        AuthenticatedUser session = SessionRepository.obtener(context);
        if (session == null || session.getUid() == null || session.getHotelId() == null
                || session.getRole() != UserRole.HOTEL_ADMIN) throw new IllegalStateException("Inicia sesión como admin.");
        uid = session.getUid(); hotelId = session.getHotelId();
        hotel = db.collection("hotels").document(String.valueOf(hotelId));
        profile = db.collection("users").document(uid);
    }
    public static boolean remote(Context context) {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }
    private void sameSession() {
        AuthenticatedUser user = SessionRepository.obtener(context);
        if (FirebaseAuth.getInstance().getCurrentUser() == null
                || !uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) || user == null
                || !uid.equals(user.getUid()) || !Objects.equals(hotelId, user.getHotelId())
                || user.getRole() != UserRole.HOTEL_ADMIN)
            throw new IllegalStateException("La sesión o el hotel asignado cambió. Inicia sesión nuevamente.");
    }
    private void assigned(Transaction tx, DocumentSnapshot currentHotel) throws FirebaseFirestoreException {
        sameSession();
        HotelAdminFirestoreCodec.assigned(uid, hotelId, tx.get(profile).getData(), currentHotel.getData());
    }
    private ManagedHotel decode(DocumentSnapshot doc) {
        if (!doc.exists() || !uid.equals(doc.getString("administratorUid")) || !Boolean.TRUE.equals(doc.getBoolean("active")))
            throw new IllegalArgumentException("No tienes un hotel activo asignado. Consulta al superadmin.");
        return HotelFirestoreCodec.decode(doc.getId(), doc.getData(), R.drawable.superadmin_hotel_hero);
    }
    public LiveData<State<ManagedHotel>> observeHotel() {
        return new LiveData<State<ManagedHotel>>(new State<>(null, null, true)) {
            ListenerRegistration listener;
            @Override protected void onActive() {
                listener = hotel.addSnapshotListener(MetadataChanges.INCLUDE, (doc, error) -> {
                    try {
                        sameSession();
                        if (error != null || doc == null || doc.getMetadata().isFromCache())
                            throw new IllegalStateException("No se confirmó el hotel. Revisa Internet y permisos.");
                        setValue(new State<>(decode(doc), null, false));
                    } catch (RuntimeException failure) { setValue(new State<>(null, failure.getMessage(), false)); }
                });
            }
            @Override protected void onInactive() { if (listener != null) listener.remove(); listener = null; setValue(new State<>(null, null, true)); }
        };
    }
    public <T> LiveData<State<List<T>>> observe(String collection, Class<T> type) {
        return androidx.lifecycle.Transformations.switchMap(observeHotel(), access -> {
            if (access.data == null) return new androidx.lifecycle.MutableLiveData<>(
                    new State<List<T>>(Collections.emptyList(), access.error, access.loading));
            return observeRows(collection, type);
        });
    }
    private <T> LiveData<State<List<T>>> observeRows(String collection, Class<T> type) {
        if (!collection.equals("rooms") && !collection.equals("services")) throw new IllegalArgumentException();
        return new LiveData<State<List<T>>>(new State<>(Collections.emptyList(), null, true)) {
            ListenerRegistration listener;
            @Override protected void onActive() {
                listener = hotel.collection(collection).addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                    try {
                        sameSession();
                        if (error != null || snapshot == null || snapshot.getMetadata().isFromCache())
                            throw new IllegalStateException("No se confirmó la lista. Revisa Internet y permisos.");
                        List<T> rows = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot) if (!Boolean.TRUE.equals(doc.getBoolean("archived")))
                            rows.add(type.cast(collection.equals("rooms") ? readRoom(doc) : readService(doc)));
                        rows.sort(Comparator.comparingInt(value -> value instanceof Habitacion ? ((Habitacion)value).id : ((Servicio)value).id));
                        setValue(new State<>(rows, null, false));
                    } catch (RuntimeException failure) { setValue(new State<>(Collections.emptyList(), failure.getMessage(), false)); }
                });
            }
            @Override protected void onInactive() { if (listener != null) listener.remove(); listener = null; setValue(new State<>(Collections.emptyList(), null, true)); }
        };
    }
    private Habitacion readRoom(DocumentSnapshot doc) { return HotelAdminFirestoreCodec.room(doc.getId(), hotelId, doc.getData(), R.drawable.superadmin_hotel_sol); }
    private Servicio readService(DocumentSnapshot doc) { return HotelAdminFirestoreCodec.service(doc.getId(), hotelId, doc.getData(), R.drawable.superadmin_hotel_sol); }
    public void loadRoom(int id, Consumer<Habitacion> success, Consumer<String> error) {
        verifiedHotel().continueWithTask(task -> {
            if (!task.isSuccessful()) return Tasks.forException(task.getException());
            return hotel.collection("rooms").document(String.valueOf(id)).get(Source.SERVER);
        }).addOnSuccessListener(doc -> {
            try { sameSession(); if (!doc.exists() || Boolean.TRUE.equals(doc.getBoolean("archived"))) throw new IllegalArgumentException("La habitación ya no está disponible."); success.accept(readRoom(doc)); }
            catch (RuntimeException failure) { error.accept(failure.getMessage()); }
        }).addOnFailureListener(failure -> error.accept("No se pudo cargar la habitación. Revisa conexión y permisos."));
    }
    public void loadService(int id, Consumer<Servicio> success, Consumer<String> error) {
        verifiedHotel().continueWithTask(task -> {
            if (!task.isSuccessful()) return Tasks.forException(task.getException());
            return hotel.collection("services").document(String.valueOf(id)).get(Source.SERVER);
        }).addOnSuccessListener(doc -> {
            try { sameSession(); if (!doc.exists() || Boolean.TRUE.equals(doc.getBoolean("archived"))) throw new IllegalArgumentException("El servicio ya no está disponible."); success.accept(readService(doc)); }
            catch (RuntimeException failure) { error.accept(failure.getMessage()); }
        }).addOnFailureListener(failure -> error.accept("No se pudo cargar el servicio. Revisa conexión y permisos."));
    }
    private Task<ManagedHotel> verifiedHotel() {
        return hotel.get(Source.SERVER).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            sameSession(); return decode(task.getResult());
        });
    }
    public static int newId() { return 1000000 + new SecureRandom().nextInt(Integer.MAX_VALUE - 1000000); }
    private void complete(Task<Void> task, Consumer<String> callback) {
        task.addOnSuccessListener(unused -> {
            try { sameSession(); callback.accept(null); } catch (RuntimeException error) { callback.accept(error.getMessage()); }
        }).addOnFailureListener(error -> {
            Throwable cause = error; while (cause.getCause() != null) cause = cause.getCause();
            callback.accept(cause instanceof IllegalArgumentException || cause instanceof IllegalStateException
                    ? cause.getMessage() : "No se guardó en Firebase. Revisa conexión, permisos y vuelve a intentarlo.");
        });
    }
    private Map<String,Object> audit(Map<String,Object> fields) {
        fields.put("updatedAt", FieldValue.serverTimestamp()); fields.put("updatedByUid", uid); return fields;
    }
    public void saveDetails(SuperadminRepository.HotelDetails details, Consumer<String> callback) {
        Map<String,Object> fields = new HashMap<>();
        fields.put("name", details.name); fields.put("address", details.address); fields.put("description", details.description);
        fields.put("attractions", details.attractions); fields.put("phone", details.phone); fields.put("email", details.email);
        complete(db.runTransaction(tx -> { DocumentSnapshot doc = tx.get(hotel); assigned(tx, doc); tx.update(hotel, audit(fields)); return null; }), callback);
    }
    public void saveService(Servicio service, boolean creating, Consumer<String> callback) {
        final Map<String,Object> fields;
        try { sameSession(); fields = HotelAdminFirestoreCodec.service(service); if (service.hotelId != hotelId) throw new IllegalArgumentException("Hotel incorrecto."); }
        catch (RuntimeException error) { callback.accept(error.getMessage()); return; }
        changeService(service.id, fields, creating, false, callback);
    }
    public void archiveService(int id, Consumer<String> callback) { changeService(id, null, false, true, callback); }
    private void changeService(int id, Map<String,Object> fields, boolean creating, boolean archive, Consumer<String> callback) {
        DocumentReference ref = hotel.collection("services").document(String.valueOf(id));
        complete(db.runTransaction(tx -> {
            DocumentSnapshot doc = tx.get(hotel); assigned(tx, doc); DocumentSnapshot current = tx.get(ref);
            if (creating ? current.exists() : !current.exists() || Boolean.TRUE.equals(current.getBoolean("archived")))
                throw new IllegalArgumentException("El servicio cambió o el ID ya existe. Recarga la lista.");
            if (archive) tx.update(ref, "activo", false, "archived", true); else tx.set(ref, fields);
            return null;
        }), callback);
    }
    public void saveRoom(Habitacion room, boolean creating, Consumer<String> callback) {
        try { sameSession(); HotelAdminFirestoreCodec.room(room); if (room.hotelId != hotelId) throw new IllegalArgumentException("Hotel incorrecto."); }
        catch (RuntimeException error) { callback.accept(error.getMessage()); return; }
        changeRoom(room.id, room, creating, false, callback);
    }
    public void archiveRoom(int id, Consumer<String> callback) { changeRoom(id, null, false, true, callback); }
    private void changeRoom(int id, Habitacion value, boolean creating, boolean archive, Consumer<String> callback) {
        // La revisión del padre detecta cambios concurrentes entre la consulta y la transacción.
        Task<Void> operation = hotel.get(Source.SERVER).continueWithTask(parent -> {
            if (!parent.isSuccessful()) return Tasks.forException(parent.getException());
            Long revision = parent.getResult().getLong("inventoryRevision"); final long expected = revision == null ? 0 : revision;
            return hotel.collection("rooms").get(Source.SERVER).continueWithTask(list -> {
                if (!list.isSuccessful()) return Tasks.forException(list.getException());
                List<Habitacion> rooms = new ArrayList<>();
                for (DocumentSnapshot doc : list.getResult()) if (!Boolean.TRUE.equals(doc.getBoolean("archived")) && !doc.getId().equals(String.valueOf(id))) rooms.add(readRoom(doc));
                if (!archive) rooms.add(value);
                Map<String,Object> summary = audit(HotelAdminFirestoreCodec.inventory(rooms));
                summary.put("inventoryRevision", expected + 1);
                DocumentReference ref = hotel.collection("rooms").document(String.valueOf(id));
                return db.<Void>runTransaction(tx -> {
                    DocumentSnapshot doc = tx.get(hotel); assigned(tx, doc);
                    Long version = doc.getLong("inventoryRevision");
                    if ((version == null ? 0 : version) != expected) throw new IllegalStateException("El inventario cambió en otro dispositivo. Recarga y vuelve a guardar.");
                    DocumentSnapshot current = tx.get(ref);
                    if (creating ? current.exists() : !current.exists() || Boolean.TRUE.equals(current.getBoolean("archived")))
                        throw new IllegalArgumentException("La habitación cambió o el ID ya existe. Recarga la lista.");
                    if (archive) tx.update(ref, "disponible", false, "archived", true); else tx.set(ref, HotelAdminFirestoreCodec.room(value));
                    tx.update(hotel, summary); return null;
                });
            });
        });
        complete(operation, callback);
    }
}
