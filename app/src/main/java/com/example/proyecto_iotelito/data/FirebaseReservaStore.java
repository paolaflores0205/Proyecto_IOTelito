package com.example.proyecto_iotelito.data;

import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.OpcionHabitaciones;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

/** Reservas del cliente autenticado. Los bloqueos por noche se crean en la misma transacción. */
final class FirebaseReservaStore {
    private static final FirebaseFirestore DB = FirebaseFirestore.getInstance();

    private FirebaseReservaStore() { }

    static LiveData<List<Reserva>> observarTodas() {
        String uid = requireUid();
        return new LiveData<List<Reserva>>() {
            private ListenerRegistration listener;

            @Override protected void onActive() {
                listener = DB.collection("reservations").whereEqualTo("clientUid", uid)
                        .addSnapshotListener((snapshot, error) -> {
                            if (error != null) {
                                Log.e("FirebaseReserva", "No se pudieron cargar las reservas", error);
                                return;
                            }
                            setValue(convertir(snapshot));
                        });
            }

            @Override protected void onInactive() {
                if (listener != null) listener.remove();
                listener = null;
            }
        };
    }

    static List<Reserva> obtenerTodas() {
        try {
            return convertir(Tasks.await(DB.collection("reservations")
                    .whereEqualTo("clientUid", requireUid()).get()));
        } catch (ExecutionException | InterruptedException error) {
            throw fallo(error);
        }
    }

    static Reserva buscarPorId(int id) {
        try {
            DocumentSnapshot document = Tasks.await(DB.collection("reservations")
                    .document(String.valueOf(id)).get());
            return document.exists() && requireUid().equals(document.getString("clientUid"))
                    ? convertir(document) : null;
        } catch (ExecutionException | InterruptedException error) {
            throw fallo(error);
        }
    }

    static Reserva agregar(Hotel hotel, LocalDate entrada, LocalDate salida, String huespedes,
                           double total, String codigo, String habitaciones, String codigos,
                           List<Integer> roomIds) {
        return agregar(hotel, entrada, salida, huespedes, total, codigo, habitaciones, codigos, roomIds, null);
    }

    static Reserva agregar(Hotel hotel, LocalDate entrada, LocalDate salida, String huespedes,
                           double total, String codigo, String habitaciones, String codigos,
                           List<Integer> roomIds, TarjetaSimulada tarjeta) {
        if (tarjeta == null) throw new IllegalArgumentException("Registra una tarjeta de prueba para confirmar la reserva.");
        String uid = requireUid();
        long noches = ChronoUnit.DAYS.between(entrada, salida);
        if (entrada.isBefore(LocalDate.now()) || noches < 1 || roomIds.isEmpty()
                || new HashSet<>(roomIds).size() != roomIds.size()
                || 1 + noches * (roomIds.size() + 1) > 400) {
            throw new IllegalArgumentException("Revisa las fechas y habitaciones seleccionadas.");
        }

        int id = ThreadLocalRandom.current().nextInt(1_000_000, Integer.MAX_VALUE);
        DocumentReference reservaRef = DB.collection("reservations").document(String.valueOf(id));
        List<DocumentReference> bloqueos = new ArrayList<>();
        for (LocalDate date = entrada; date.isBefore(salida); date = date.plusDays(1)) {
            bloqueos.add(DB.collection("clientNights").document(uid + "_" + date));
            for (int roomId : roomIds) {
                bloqueos.add(DB.collection("roomNights")
                        .document(hotel.id + "_" + roomId + "_" + date));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.putAll(tarjeta.campos());
        data.put("id", id);
        data.put("clientUid", uid);
        data.put("clientName", com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getDisplayName());
        data.put("hotelName", hotel.name);
        data.put("hotelId", hotel.id);
        data.put("roomIds", roomIds);
        data.put("roomName", habitaciones);
        data.put("roomNumber", codigos);
        data.put("fechaEntrada", entrada.toString());
        data.put("fechaSalida", salida.toString());
        data.put("huespedes", huespedes);
        data.put("precioTotal", total);
        data.put("estado", Reserva.Estado.CONFIRMADA.name());
        data.put("codigo", codigo);
        data.put("createdAt", FieldValue.serverTimestamp());

        try {
            return Tasks.await(DB.runTransaction(transaction -> {
                DocumentSnapshot currentHotel = transaction.get(DB.collection("hotels").document(String.valueOf(hotel.id)));
                if (!currentHotel.exists() || !Boolean.TRUE.equals(currentHotel.getBoolean("active"))
                        || !Boolean.TRUE.equals(currentHotel.getBoolean("hasInventory")))
                    throw new IllegalArgumentException("El hotel ya no está disponible.");
                for (int roomId : roomIds) {
                    DocumentSnapshot room = transaction.get(DB.collection("hotels").document(String.valueOf(hotel.id))
                            .collection("rooms").document(String.valueOf(roomId)));
                    if (!room.exists() || !Boolean.TRUE.equals(room.getBoolean("disponible")))
                        throw new IllegalArgumentException("Una habitación ya no está disponible.");
                }
                if (transaction.get(reservaRef).exists()) {
                    throw new IllegalArgumentException("Intenta confirmar la reserva nuevamente.");
                }
                for (DocumentReference bloqueo : bloqueos) {
                    if (transaction.get(bloqueo).exists()) {
                        throw new IllegalArgumentException(
                                bloqueo.getPath().startsWith("clientNights/")
                                        ? "Ya tienes una reserva que se cruza con estas fechas. Elige otro período, aunque la habitación esté libre."
                                        : "Esta habitación ya fue reservada para esas fechas.");
                    }
                }
                transaction.set(reservaRef, data);
                for (DocumentReference bloqueo : bloqueos) {
                    Map<String, Object> lock = new HashMap<>();
                    lock.put("reservationId", String.valueOf(id));
                    if (bloqueo.getPath().startsWith("clientNights/")) {
                        lock.put("clientUid", uid);
                    }
                    transaction.set(bloqueo, lock);
                }
                return new Reserva(id, hotel.id, habitaciones, codigos, entrada, salida,
                        huespedes, total, Reserva.Estado.CONFIRMADA, codigo);
            }));
        } catch (ExecutionException | InterruptedException error) {
            throw fallo(error);
        }
    }

    static List<OpcionHabitaciones> filtrarDisponibles(int hotelId, LocalDate entrada,
                                                       LocalDate salida,
                                                       List<OpcionHabitaciones> opciones) {
        String uid = requireUid();
        if (!entrada.isBefore(salida)) throw new IllegalArgumentException("Revisa las fechas de tu búsqueda.");
        for (LocalDate date = entrada; date.isBefore(salida); date = date.plusDays(1)) {
            try {
                if (Tasks.await(DB.collection("clientNights").document(uid + "_" + date)
                        .get(com.google.firebase.firestore.Source.SERVER)).exists())
                    throw new IllegalArgumentException("Ya tienes una reserva que se cruza con estas fechas. Elige otro período, aunque la habitación esté libre.");
            } catch (ExecutionException | InterruptedException error) {
                throw fallo(error);
            }
        }
        Set<String> keys = new java.util.LinkedHashSet<>();
        for (OpcionHabitaciones opcion : opciones) for (Habitacion room : opcion.habitaciones())
            for (LocalDate date = entrada; date.isBefore(salida); date = date.plusDays(1))
                keys.add(hotelId + "_" + room.id + "_" + date);
        Map<String, Boolean> ocupadas = new HashMap<>();
        List<String> noches = new ArrayList<>(keys);
        try {
            // Lecturas independientes en grupos acotados; nunca usar caché para confirmar disponibilidad.
            for (int start = 0; start < noches.size(); start += 20) {
                List<com.google.android.gms.tasks.Task<DocumentSnapshot>> lecturas = new ArrayList<>();
                for (int i = start; i < Math.min(start + 20, noches.size()); i++)
                    lecturas.add(DB.collection("roomNights").document(noches.get(i))
                            .get(com.google.firebase.firestore.Source.SERVER));
                for (DocumentSnapshot doc : Tasks.await(Tasks.<DocumentSnapshot>whenAllSuccess(lecturas)))
                    ocupadas.put(doc.getId(), doc.exists());
                if (!uid.equals(requireUid())) throw new IllegalStateException("La sesión cambió. Vuelve a buscar.");
            }
        } catch (ExecutionException | InterruptedException error) { throw fallo(error); }
        List<OpcionHabitaciones> disponibles = new ArrayList<>();
        for (OpcionHabitaciones opcion : opciones) {
            boolean libre = true;
            for (Habitacion room : opcion.habitaciones()) {
                for (LocalDate date = entrada; date.isBefore(salida); date = date.plusDays(1)) {
                    if (Boolean.TRUE.equals(ocupadas.get(hotelId + "_" + room.id + "_" + date))) {
                        libre = false;
                        break;
                    }
                }
                if (!libre) break;
            }
            if (libre) disponibles.add(opcion);
        }
        return disponibles;
    }

    static void actualizarEstado(int id, Reserva.Estado estado) {
        throw new IllegalArgumentException("Solicita el checkout; el cierre corresponde al administrador.");
    }

    static void solicitarCheckout(int id, int estrellas, String comentario, TarjetaSimulada tarjeta) {
        String uid = requireUid();
        CheckoutPolicy.validarOpinion(estrellas, comentario);
        DocumentReference ref = DB.collection("reservations").document(String.valueOf(id));
        try {
            Tasks.await(DB.runTransaction(tx -> {
                DocumentSnapshot before = tx.get(ref);
                if (!uid.equals(requireUid()) || !before.exists() || !uid.equals(before.getString("clientUid")))
                    throw new IllegalArgumentException("No se encontró una reserva propia.");
                String estado = before.getString("estado");
                if ("EN_CHECKOUT".equals(estado) || CheckoutPolicy.finalizada(estado)) return null;
                if (!CheckoutPolicy.puedeSolicitar(estado, LocalDate.parse(before.getString("fechaSalida")),
                        LocalDate.now(java.time.ZoneId.of("America/Lima"))))
                    throw new IllegalArgumentException("El checkout está disponible desde el día de salida.");
                TarjetaSimulada existing = TarjetaSimulada.leer(before.getData());
                if (existing == null && tarjeta == null) throw new IllegalArgumentException("Registra una tarjeta de prueba.");
                Map<String,Object> changes = new HashMap<>();
                changes.put("estado", "EN_CHECKOUT"); changes.put("rating", estrellas);
                changes.put("review", comentario.trim()); changes.put("checkoutRequestedAt", FieldValue.serverTimestamp());
                if (existing == null) changes.putAll(tarjeta.campos());
                tx.update(ref, changes);
                return null;
            }));
        } catch (ExecutionException | InterruptedException error) { throw fallo(error); }
    }

    static ComprobanteCheckout comprobante(int id) {
        String uid = requireUid();
        try {
            DocumentSnapshot booking = Tasks.await(DB.collection("reservations").document(String.valueOf(id))
                    .get(com.google.firebase.firestore.Source.SERVER));
            if (!booking.exists() || !uid.equals(booking.getString("clientUid")))
                throw new IllegalArgumentException("No se encontró tu reserva.");
            DocumentSnapshot doc = Tasks.await(DB.collection("hotelCheckouts").document(String.valueOf(id))
                    .get(com.google.firebase.firestore.Source.SERVER));
            if (!uid.equals(requireUid())) throw new IllegalArgumentException("La sesión cambió.");
            if (!doc.exists()) return null;
            if (!uid.equals(doc.getString("clientUid")) || !CheckoutPolicy.finalizada(booking.getString("estado"))
                    || !String.valueOf(id).equals(doc.getString("reservationId"))
                    || !java.util.Objects.equals(booking.getLong("hotelId"), doc.getLong("hotelId")))
                throw new IllegalArgumentException("El comprobante no corresponde a una reserva finalizada propia.");
            com.google.firebase.Timestamp fecha = doc.getTimestamp("createdAt");
            if (fecha == null) throw new IllegalArgumentException("El comprobante aún no fue confirmado por el servidor.");
            return new ComprobanteCheckout(doc.getData(), fecha.toDate().getTime());
        } catch (ExecutionException | InterruptedException error) { throw fallo(error); }
    }

    private static List<Reserva> convertir(QuerySnapshot snapshot) {
        List<Reserva> reservas = new ArrayList<>();
        if (snapshot == null) return reservas;
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            try { reservas.add(convertir(document)); }
            catch (RuntimeException error) {
                Log.e("FirebaseReserva", "Reserva inválida: " + document.getId(), error);
            }
        }
        return reservas;
    }

    private static Reserva convertir(DocumentSnapshot document) {
        Number id = (Number) document.get("id");
        Number hotelId = (Number) document.get("hotelId");
        Number total = (Number) document.get("precioTotal");
        Reserva reserva = new Reserva(id.intValue(), hotelId.intValue(),
                document.getString("roomName"), document.getString("roomNumber"),
                LocalDate.parse(document.getString("fechaEntrada")),
                LocalDate.parse(document.getString("fechaSalida")),
                document.getString("huespedes"), total.doubleValue(),
                Reserva.Estado.desdeFirestore(document.getString("estado")),
                document.getString("codigo"));
        reserva.tarjeta = TarjetaSimulada.leer(document.getData());
        Long rating = document.getLong("rating");
        reserva.valoracion = rating == null ? null : rating.intValue();
        reserva.observaciones = document.getString("review");
        return reserva;
    }

    private static String requireUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) throw new IllegalStateException("Inicia sesión para ver tus reservas.");
        return user.getUid();
    }

    private static RuntimeException fallo(Exception error) {
        if (error instanceof InterruptedException) Thread.currentThread().interrupt();
        Throwable cause = error;
        while (cause.getCause() != null) cause = cause.getCause();
        if (cause instanceof IllegalArgumentException) return (IllegalArgumentException) cause;
        if (cause instanceof FirebaseFirestoreException) {
            FirebaseFirestoreException firestoreError = (FirebaseFirestoreException) cause;
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                return new IllegalArgumentException(
                        "Firestore rechazó el acceso. Revisa las reglas publicadas de reservas.", error);
            }
            if (firestoreError.getCode() == FirebaseFirestoreException.Code.UNAVAILABLE) {
                return new IllegalArgumentException(
                        "No se pudo conectar con Firestore. Revisa tu conexión.", error);
            }
        }
        return new IllegalStateException("No se pudo conectar con Firestore. Intenta nuevamente.", error);
    }
}
