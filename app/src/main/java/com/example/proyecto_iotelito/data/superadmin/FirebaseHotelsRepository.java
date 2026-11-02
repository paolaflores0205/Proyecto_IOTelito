package com.example.proyecto_iotelito.data.superadmin;

import android.content.Context;
import androidx.lifecycle.LiveData;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;
import com.example.proyecto_iotelito.model.auth.RemoteUserProfile;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import com.google.android.gms.tasks.Tasks;
import java.util.*;
import java.util.concurrent.ExecutionException;

/** Firestore es la fuente remota; Room se conserva únicamente como catálogo para importación explícita. */
public final class FirebaseHotelsRepository {
    public static final class State {
        public final List<ManagedHotel> hotels;
        public final String error;
        public final boolean loading;
        State(List<ManagedHotel> hotels, boolean loading, String error) {
            this.hotels = Collections.unmodifiableList(new ArrayList<>(hotels)); this.loading = loading; this.error = error;
        }
    }
    private final Context context;
    private final boolean showPrivatePhotos;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    public FirebaseHotelsRepository(Context context, boolean showPrivatePhotos) {
        this.context = context.getApplicationContext(); this.showPrivatePhotos = showPrivatePhotos;
    }
    private static String actor() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalArgumentException("Inicia sesión con Firebase.");
        return user.getUid();
    }
    private String galleryKey(int id) { return actor() + ":" + id; }
    private ManagedHotel decode(DocumentSnapshot document) {
        Number id = (Number) document.get("id");
        int image = id != null && id.intValue() >= 1 && id.intValue() <= 12
                ? HotelMedia.hotelImage(id.intValue()) : R.drawable.superadmin_hotel_hero;
        ManagedHotel hotel = HotelFirestoreCodec.decode(document.getId(), document.getData(), image);
        if (showPrivatePhotos) {
            String raw = context.getSharedPreferences("hotel_private_galleries", Context.MODE_PRIVATE)
                    .getString(galleryKey(hotel.id), "[]");
            try {
                org.json.JSONArray local = new org.json.JSONArray(raw);
                for (int i = 0; i < local.length() && hotel.photos.size() < 8; i++) hotel.photos.add(local.getString(i));
            } catch (org.json.JSONException ignored) { }
        }
        return hotel;
    }
    private void savePrivatePhotos(String uid, int id, List<String> photos) {
        List<String> shared = HotelFirestoreCodec.sharedPhotos(photos), local = new ArrayList<>();
        for (String photo : photos) if (photo != null && !shared.contains(photo) && (photo.startsWith("file:") || photo.startsWith("content:"))) local.add(photo);
        context.getSharedPreferences("hotel_private_galleries", Context.MODE_PRIVATE).edit()
                .putString(uid + ":" + id, new org.json.JSONArray(local).toString()).apply();
    }
    public LiveData<State> observe() {
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
                        setValue(new State(Collections.emptyList(), false, "Inicia sesión para consultar hoteles.")); return;
                    }
                    String uid = auth.getCurrentUser().getUid();
                    listener = db.collection("hotels").addSnapshotListener(MetadataChanges.INCLUDE, (snapshot, error) -> {
                        if (auth.getCurrentUser() == null || !uid.equals(auth.getCurrentUser().getUid())) return;
                        if (error != null || snapshot == null || snapshot.getMetadata().isFromCache()) {
                            setValue(new State(Collections.emptyList(), false,
                                    "No se confirmó el catálogo remoto. Revisa conexión, perfil y reglas de Firestore.")); return;
                        }
                        List<ManagedHotel> hotels = new ArrayList<>(); int invalid = 0;
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try { hotels.add(decode(doc)); } catch (RuntimeException malformed) { invalid++; }
                        }
                        hotels.sort(Comparator.comparing(h -> h.name.toLowerCase(Locale.ROOT)));
                        setValue(new State(hotels, false, invalid == 0 ? null : invalid + " hoteles tienen datos incompletos en Firestore."));
                    });
                };
                FirebaseAuth.getInstance().addAuthStateListener(authListener);
            }
            @Override protected void onInactive() {
                if (listener != null) listener.remove(); listener = null;
                if (authListener != null) FirebaseAuth.getInstance().removeAuthStateListener(authListener);
                authListener = null;
                setValue(new State(Collections.emptyList(), true, null));
            }
        };
    }
    public ManagedHotel find(int id) throws InterruptedException {
        try {
            DocumentSnapshot doc = Tasks.await(db.collection("hotels").document(String.valueOf(id)).get(Source.SERVER));
            return doc.exists() ? decode(doc) : null;
        } catch (ExecutionException error) { throw new IllegalStateException("No se pudo cargar el hotel remoto.", error); }
    }
    public List<Habitacion> rooms(int hotelId) throws InterruptedException {
        try {
            QuerySnapshot snapshot = Tasks.await(db.collection("hotels").document(String.valueOf(hotelId)).collection("rooms").get(Source.SERVER));
            List<Habitacion> result = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot) {
                Map<String, Object> data = doc.getData();
                int id = HotelFirestoreCodec.integer(data.get("id"));
                if (!String.valueOf(id).equals(doc.getId()) || HotelFirestoreCodec.integer(data.get("hotelId")) != hotelId)
                    throw new IllegalArgumentException("Inventario remoto con IDs inconsistentes.");
                if (!Boolean.TRUE.equals(data.get("disponible"))) continue;
                result.add(new Habitacion(id, hotelId, String.valueOf(data.get("tipo")), String.valueOf(data.get("nombreCodigo")),
                        HotelFirestoreCodec.integer(data.get("areaM2")), HotelFirestoreCodec.integer(data.get("adultos")),
                        HotelFirestoreCodec.integer(data.get("ninos")), ((Number) data.get("precioNoche")).doubleValue(),
                        String.valueOf(data.get("descripcion")), R.drawable.superadmin_hotel_hero, true));
            }
            return result;
        } catch (ExecutionException error) { throw new IllegalStateException("No se pudo cargar el inventario remoto.", error); }
    }
    private void complete(com.google.android.gms.tasks.Task<Integer> task, SuperadminRepository.Callback callback) {
        task.addOnSuccessListener(id -> callback.complete(id, null)).addOnFailureListener(error -> {
            Throwable cause = error;
            while (cause.getCause() != null) cause = cause.getCause();
            callback.complete(0, cause instanceof IllegalArgumentException ? cause.getMessage()
                    : "No se guardó en Firebase. Revisa conexión, permisos y datos del hotel.");
        });
    }
    public void save(ManagedHotel hotel, SuperadminRepository.Callback callback) {
        final String uid;
        final Map<String, Object> edits;
        try { uid = actor(); edits = HotelFirestoreCodec.editable(hotel, false); }
        catch (IllegalArgumentException error) { callback.complete(0, error.getMessage()); return; }
        edits.put("updatedAt", FieldValue.serverTimestamp()); edits.put("updatedByUid", uid);
        complete(db.runTransaction(tx -> {
            if (hotel.id != 0) {
                DocumentReference ref = db.collection("hotels").document(String.valueOf(hotel.id));
                if (!tx.get(ref).exists()) throw new IllegalArgumentException("El hotel remoto ya no existe.");
                tx.update(ref, edits); return hotel.id;
            }
            DocumentReference counter = db.collection("system").document("hotelIds");
            DocumentSnapshot sequence = tx.get(counter);
            int id = sequence.exists() ? HotelFirestoreCodec.integer(sequence.get("nextId")) : HotelFirestoreCodec.FIRST_NEW_ID;
            if (id < HotelFirestoreCodec.FIRST_NEW_ID || id == Integer.MAX_VALUE) throw new IllegalArgumentException("Contador de hoteles fuera de rango.");
            DocumentReference ref = db.collection("hotels").document(String.valueOf(id));
            if (tx.get(ref).exists()) throw new IllegalArgumentException("El ID asignado ya existe. Solicita revisar el contador; no se sobrescribió el hotel.");
            Map<String, Object> data = newHotel(edits, id, uid, "APP");
            tx.set(ref, data);
            Map<String, Object> next = new HashMap<>(); next.put("nextId", id + 1); next.put("updatedAt", FieldValue.serverTimestamp());
            next.put("updatedByUid", uid); tx.set(counter, next);
            return id;
        }).addOnSuccessListener(id -> savePrivatePhotos(uid, id, hotel.photos)), callback);
    }
    private static Map<String, Object> newHotel(Map<String, Object> edits, int id, String uid, String origin) {
        Map<String, Object> data = new HashMap<>(edits);
        data.put("id", id); data.put("administratorUid", null); data.put("administratorName", null);
        data.put("rooms", 0); data.put("reservations", 0); data.put("hasInventory", false);
        data.put("pricePerNight", 0); data.put("rating", 0); data.put("reviews", 0);
        data.put("services", Collections.emptyList()); data.put("attractions", "");
        data.put("dataOrigin", origin); data.put("createdAt", FieldValue.serverTimestamp()); data.put("createdByUid", uid);
        data.put("updatedAt", FieldValue.serverTimestamp()); data.put("updatedByUid", uid);
        return data;
    }
    public void active(int id, boolean active, SuperadminRepository.Callback callback) {
        final String uid;
        try { uid = actor(); } catch (IllegalArgumentException error) { callback.complete(0, error.getMessage()); return; }
        Map<String, Object> changes = new HashMap<>(); changes.put("active", active);
        changes.put("updatedAt", FieldValue.serverTimestamp()); changes.put("updatedByUid", uid);
        complete(db.collection("hotels").document(String.valueOf(id)).update(changes).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException(); return id;
        }), callback);
    }
    public void assign(int hotelId, String targetUid, SuperadminRepository.Callback callback) {
        final String uid;
        try { uid = actor(); } catch (IllegalArgumentException error) { callback.complete(0, error.getMessage()); return; }
        DocumentReference hotelRef = db.collection("hotels").document(String.valueOf(hotelId));
        complete(db.runTransaction(tx -> {
            DocumentSnapshot hotel = tx.get(hotelRef);
            if (!hotel.exists()) throw new IllegalArgumentException("El hotel ya no existe.");
            String oldUid = hotel.getString("administratorUid");
            DocumentReference oldRef = oldUid == null ? null : db.collection("users").document(oldUid);
            DocumentSnapshot old = oldRef == null ? null : tx.get(oldRef);
            DocumentReference targetRef = targetUid == null ? null : db.collection("users").document(targetUid);
            DocumentSnapshot target = targetRef == null ? null : tx.get(targetRef);
            RemoteUserProfile profile = null;
            if (target != null) {
                profile = RemoteUserProfile.parse(targetUid, target.getData());
                Number assigned = (Number) target.get("hotelId");
                if (!profile.active || profile.role != UserRole.HOTEL_ADMIN)
                    throw new IllegalArgumentException("Selecciona un administrador Firebase activo.");
                if (assigned != null && assigned.intValue() != hotelId)
                    throw new IllegalArgumentException("Ese administrador ya está asignado a otro hotel. Desvincúlalo primero.");
            }
            Map<String, Object> link = new HashMap<>(); link.put("hotelId", hotelId);
            link.put("updatedAt", FieldValue.serverTimestamp()); link.put("updatedByUid", uid);
            if (old != null && old.exists() && !Objects.equals(oldUid, targetUid)) {
                Map<String, Object> unlink = new HashMap<>(link); unlink.put("hotelId", null); tx.update(oldRef, unlink);
            }
            if (targetRef != null) tx.update(targetRef, link);
            Map<String, Object> changes = new HashMap<>(); changes.put("administratorUid", targetUid);
            changes.put("administratorName", profile == null ? null : profile.name);
            changes.put("updatedAt", FieldValue.serverTimestamp()); changes.put("updatedByUid", uid);
            tx.update(hotelRef, changes); return hotelId;
        }), callback);
    }
    public void importLocal(List<ManagedHotel> hotels, SuperadminRepository.Callback callback) {
        final String uid;
        try { uid = actor(); } catch (IllegalArgumentException error) { callback.complete(0, error.getMessage()); return; }
        // Una transacción por hotel: hotel + inventario se publican juntos o no se publica nada.
        com.google.android.gms.tasks.Task<Integer> chain = Tasks.forResult(0);
        for (ManagedHotel hotel : hotels) {
            chain = chain.onSuccessTask(count -> db.runTransaction(tx -> {
                if (hotel.id <= 0 || hotel.id >= HotelFirestoreCodec.FIRST_NEW_ID)
                    throw new IllegalArgumentException("La importación admite solo IDs del catálogo antiguo.");
                DocumentReference ref = db.collection("hotels").document(String.valueOf(hotel.id));
                if (tx.get(ref).exists()) return count;
                List<Habitacion> rooms = CatalogoHabitaciones.porHotel(hotel.id);
                if (rooms.size() > 50) throw new IllegalArgumentException("Divide la importación de inventarios grandes.");
                for (Habitacion room : rooms) if (tx.get(ref.collection("rooms").document(String.valueOf(room.id))).exists())
                    throw new IllegalArgumentException("Hay habitaciones remotas sin hotel. Revisa esos datos antes de importar.");
                Map<String, Object> data = newHotel(HotelFirestoreCodec.editable(hotel, true), hotel.id, uid, "LOCAL_IMPORT");
                com.example.proyecto_iotelito.model.Hotel legacy = HotelCatalogRepository.combinar(hotel);
                data.put("services", Arrays.asList(legacy.services));
                data.put("pricePerNight", rooms.stream().filter(r -> r.disponible).mapToDouble(r -> r.precioNoche).min().orElse(0));
                data.put("rooms", rooms.size()); data.put("hasInventory", rooms.stream().anyMatch(r -> r.disponible));
                data.put("attractions", hotel.attractions == null ? "" : hotel.attractions);
                tx.set(ref, data);
                for (Habitacion room : rooms) {
                    Map<String, Object> item = new HashMap<>(); item.put("id", room.id); item.put("hotelId", hotel.id);
                    item.put("tipo", room.tipo); item.put("nombreCodigo", room.nombreCodigo); item.put("areaM2", room.areaM2);
                    item.put("adultos", room.adultos); item.put("ninos", room.ninos); item.put("precioNoche", room.precioNoche);
                    item.put("descripcion", room.descripcion); item.put("disponible", room.disponible);
                    tx.set(ref.collection("rooms").document(String.valueOf(room.id)), item);
                }
                return count + 1;
            }).addOnSuccessListener(newCount -> { if (newCount > count) savePrivatePhotos(uid, hotel.id, hotel.photos); }));
        }
        complete(chain, (count, error) -> callback.complete(count, error == null ? null
                : "La importación pudo quedar parcial. Repite para completar sin sobrescribir existentes. " + error));
    }
}
