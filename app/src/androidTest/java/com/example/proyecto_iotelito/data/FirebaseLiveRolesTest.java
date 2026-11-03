package com.example.proyecto_iotelito.data;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository;
import com.example.proyecto_iotelito.data.superadmin.*;
import com.example.proyecto_iotelito.data.taxista.FirebaseTaxiStore;
import com.example.proyecto_iotelito.model.*;
import com.example.proyecto_iotelito.model.auth.*;
import com.example.proyecto_iotelito.model.hoteladmin.*;
import com.google.android.gms.tasks.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import org.json.*;
import org.junit.Test;
import org.junit.After;
import org.junit.runner.RunWith;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeNotNull;

/** Prueba OPTATIVA contra cuentas reales: no contiene ni persiste credenciales.
 * Requiere qaAccounts (JSON base64) y qaWrite=true para crear registros QA.
 * Las reservas/solicitudes no se borran: quedan identificadas como pruebas.
 */
@RunWith(AndroidJUnit4.class)
public class FirebaseLiveRolesTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    private final Context context = ins.getTargetContext();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private JSONObject accounts;
    private boolean sessionUsed;
    @After public void closeTestSession() {
        if (sessionUsed) ins.runOnMainSync(() -> SessionRepository.cerrar(context)); accounts = null;
    }
    private AuthenticatedUser login(UserRole role) throws Exception {
        sessionUsed = true;
        ins.runOnMainSync(() -> SessionRepository.cerrar(context));
        JSONObject account = accounts.getJSONObject(role.name());
        Tasks.await(FirebaseAuth.getInstance().signInWithEmailAndPassword(account.getString("email"),
                account.getString("password")), 30, TimeUnit.SECONDS);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<AuthenticatedUser> profile = new AtomicReference<>();
        AtomicReference<String> error = new AtomicReference<>();
        ins.runOnMainSync(() -> FirebaseProfileRepository.loadCurrent((user, message) -> {
            profile.set(user); error.set(message); latch.countDown();
        }));
        assertTrue(role + ": tiempo de consulta", latch.await(30, TimeUnit.SECONDS));
        assertNotNull(role + ": " + error.get(), profile.get());
        assertEquals(role + ": rol recibido", role, profile.get().getRole());
        ins.runOnMainSync(() -> SessionRepository.guardar(context, profile.get()));
        return profile.get();
    }
    private DocumentSnapshot get(String path) throws Exception {
        return Tasks.await(db.document(path).get(Source.SERVER), 30, TimeUnit.SECONDS);
    }
    private void operation(Consumer<Consumer<String>> start) throws Exception {
        CountDownLatch latch = new CountDownLatch(1); AtomicReference<String> error = new AtomicReference<>();
        ins.runOnMainSync(() -> start.accept(message -> { error.set(message); latch.countDown(); }));
        assertTrue("Tiempo de operación", latch.await(40, TimeUnit.SECONDS));
        assertNull(error.get(), error.get());
    }
    private static void result(String message) { System.out.println("LIVE_QA " + message); }

    @Test public void verifyRolesAndOptionalSharedFlow() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments(); String encoded = args.getString("qaAccounts");
        assumeNotNull(encoded);
        accounts = new JSONObject(new String(Base64.decode(encoded, Base64.DEFAULT), java.nio.charset.StandardCharsets.UTF_8));
        boolean excludeSuperadmin = "true".equals(args.getString("qaExcludeSuperadmin"));
        int failures = 0;
        for (UserRole role : UserRole.values()) {
            if (excludeSuperadmin && role == UserRole.SUPERADMIN) { result("SUPERADMIN pendiente_credencial_valida"); continue; }
            try {
                AuthenticatedUser user = login(role);
                if (role == UserRole.SUPERADMIN) {
                    QuerySnapshot users = Tasks.await(db.collection("users").get(Source.SERVER), 30, TimeUnit.SECONDS);
                    QuerySnapshot hotels = Tasks.await(db.collection("hotels").get(Source.SERVER), 30, TimeUnit.SECONDS);
                    result("SUPERADMIN usuarios=" + users.size() + " hoteles=" + hotels.size());
                } else if (role == UserRole.HOTEL_ADMIN) {
                    assertNotNull("Hotel asignado", user.getHotelId());
                    DocumentSnapshot hotel = get("hotels/" + user.getHotelId());
                    assertEquals(user.getUid(), hotel.getString("administratorUid"));
                    assertEquals(Boolean.TRUE, hotel.getBoolean("active"));
                    result("HOTEL_ADMIN hotel=" + user.getHotelId() + " asignacion_consistente");
                } else if (role == UserRole.TAXISTA) {
                    DocumentSnapshot driver = get("taxistas/" + user.getUid());
                    assertEquals(Boolean.TRUE, driver.getBoolean("activo"));
                    assertTrue(driver.get("disponible") instanceof Boolean);
                    result("TAXISTA perfil_operativo_valido");
                } else {
                    QuerySnapshot bookings = Tasks.await(db.collection("reservations").whereEqualTo("clientUid", user.getUid())
                            .get(Source.SERVER), 30, TimeUnit.SECONDS);
                    result("CLIENT acceso_valido reservas_propias=" + bookings.size());
                }
            } catch (Exception | AssertionError error) {
                failures++; result(role + " FALLO " + error.getClass().getSimpleName() + " " + safe(error));
            }
        }
        assertEquals("Hay roles sin acceso/configuración correcta. Revisar LIVE_QA.", 0, failures);
        if (!"true".equals(args.getString("qaWrite"))) {
            ins.runOnMainSync(() -> SessionRepository.cerrar(context)); return;
        }

        // No tocar un servicio real en curso ni cambiar su conductor.
        AuthenticatedUser driver = login(UserRole.TAXISTA);
        DocumentSnapshot driverLock = get("taxiDrivers/" + driver.getUid());
        assertTrue("El conductor tiene otro servicio: prueba detenida sin modificarlo", !driverLock.exists()
                || driverLock.getString("activeRequestId") == null || driverLock.getString("activeRequestId").isEmpty());
        boolean originalAvailability = Boolean.TRUE.equals(get("taxistas/" + driver.getUid()).getBoolean("disponible"));
        AuthenticatedUser admin = login(UserRole.HOTEL_ADMIN);
        int hotelId = admin.getHotelId(); int roomId = FirebaseHotelAdminRepository.newId();
        int serviceId = FirebaseHotelAdminRepository.newId();
        String marker = "QA-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT);
        FirebaseHotelAdminRepository adminRepo = new FirebaseHotelAdminRepository(context);
        boolean roomCreated = false, serviceCreated = false;
        Integer createdTaxiId = null;
        boolean availabilityChanged = false;
        try {
            Habitacion room = new Habitacion(roomId, hotelId, "Prueba QA", marker, 24, 2, 0, 151.50,
                    "Registro de prueba de integración; no es inventario comercial.", R.drawable.superadmin_hotel_hero, true);
            operation(done -> adminRepo.saveRoom(room, true, done)); roomCreated = true;
            result("HABITACION creada id=" + roomId);
            Habitacion edited = new Habitacion(roomId, hotelId, "Prueba QA", marker, 24, 2, 0, 175.75,
                    room.descripcion, room.fotoRes, true);
            operation(done -> adminRepo.saveRoom(edited, false, done));
            assertEquals(175.75, get("hotels/" + hotelId + "/rooms/" + roomId).getDouble("precioNoche"), .001);
            result("HABITACION precio_editado_confirmado");
            Servicio service = new Servicio(serviceId, hotelId, marker + " Servicio", "Servicio de prueba QA", 12.50,
                    "servicio", R.drawable.superadmin_hotel_hero, true);
            operation(done -> adminRepo.saveService(service, true, done)); serviceCreated = true;
            Servicio updatedService = new Servicio(serviceId, hotelId, service.nombre, service.descripcion, 15.75,
                    service.unidad, service.fotoRes, true);
            operation(done -> adminRepo.saveService(updatedService, false, done));
            assertEquals(15.75, get("hotels/" + hotelId + "/services/" + serviceId).getDouble("precio"), .001);
            result("SERVICIO alta_y_edicion_confirmadas id=" + serviceId);

            AuthenticatedUser client = login(UserRole.CLIENT);
            Hotel hotel = HotelCatalogRepository.buscar(context, hotelId);
            assertNotNull(hotel);
            assertTrue(hotel.inventory.stream().anyMatch(r -> r.id == roomId && r.precioNoche == 175.75));
            LocalDate arrival = LocalDate.now().plusDays(90);
            for (int offset = 0; offset < 60; offset++) {
                if (!get("clientNights/" + client.getUid() + "_" + arrival).exists()) break;
                arrival = arrival.plusDays(1);
            }
            Reserva booking = FirebaseReservaStore.agregar(hotel, arrival, arrival.plusDays(1), "2 adultos · PRUEBA QA",
                    175.75, marker, "Prueba QA", marker, Collections.singletonList(roomId));
            assertTrue(get("reservations/" + booking.id).exists());
            result("RESERVA id=" + booking.id + " hotel=" + hotelId + " entrada=" + arrival + " codigo=" + marker);
            int taxiId = FirebaseTaxiStore.crear(booking, "PRUEBA QA", hotel.name, hotel.address, "14:30", 2, 1);
            createdTaxiId = taxiId;
            result("TAXI solicitud_creada id=" + taxiId);
            driver = login(UserRole.TAXISTA);
            final String driverUid = driver.getUid();
            Tasks.await(db.runTransaction(tx -> {
                DocumentReference ref = db.collection("taxistas").document(driverUid); tx.get(ref);
                tx.update(ref, "disponible", true); return null;
            }), 30, TimeUnit.SECONDS);
            availabilityChanged = true;
            for (String state : Arrays.asList("ASIGNADO", "EN_CAMINO", "EN_TRASLADO", "FINALIZADO")) {
                assertTrue("Estado " + state, FirebaseTaxiStore.avanzar(driverUid, taxiId, state, driver.getEmail(), "PRUEBA QA"));
                assertEquals(state, get("taxiRequests/" + taxiId).getString("estado")); result("TAXI " + state);
            }
            assertEquals("", get("taxiDrivers/" + driverUid).getString("activeRequestId"));
            Tasks.await(db.collection("taxistas").document(driverUid).update("disponible", originalAvailability), 30, TimeUnit.SECONDS);
            login(UserRole.CLIENT);
            assertEquals("FINALIZADO", get("taxiRequests/" + taxiId).getString("estado"));
            result("CLIENT estado_final_compartido_confirmado");
            if (!excludeSuperadmin) {
                login(UserRole.SUPERADMIN);
                QuerySnapshot reservations = Tasks.await(db.collection("reservations").get(Source.SERVER), 30, TimeUnit.SECONDS);
                assertTrue(reservations.getDocuments().stream().anyMatch(doc -> doc.getId().equals(String.valueOf(booking.id))));
                result("SUPERADMIN reserva_visible_para_reportes");
            }
        } finally {
            try {
                if (availabilityChanged) {
                    try {
                        AuthenticatedUser cleanupDriver = login(UserRole.TAXISTA);
                        if (createdTaxiId != null) {
                            DocumentSnapshot lock = get("taxiDrivers/" + cleanupDriver.getUid());
                            if (lock.exists() && String.valueOf(createdTaxiId).equals(lock.getString("activeRequestId"))) {
                                String current = get("taxiRequests/" + createdTaxiId).getString("estado");
                                List<String> sequence = Arrays.asList("ASIGNADO", "EN_CAMINO", "EN_TRASLADO", "FINALIZADO");
                                int position = sequence.indexOf(current);
                                if (position >= 0) for (int next = position + 1; next < sequence.size(); next++)
                                    assertTrue(FirebaseTaxiStore.avanzar(cleanupDriver.getUid(), createdTaxiId, sequence.get(next), cleanupDriver.getEmail(), "PRUEBA QA"));
                            }
                        }
                        Tasks.await(db.collection("taxistas").document(cleanupDriver.getUid()).update("disponible", originalAvailability), 30, TimeUnit.SECONDS);
                        result("TAXISTA disponibilidad_original_restaurada");
                    } catch (Exception | AssertionError cleanupError) { result("ATENCION limpieza_taxi " + safe(cleanupError)); }
                }
                login(UserRole.HOTEL_ADMIN);
                FirebaseHotelAdminRepository cleanup = new FirebaseHotelAdminRepository(context);
                if (serviceCreated) { operation(done -> cleanup.archiveService(serviceId, done)); result("SERVICIO archivado=" + serviceId); }
                if (roomCreated) { operation(done -> cleanup.archiveRoom(roomId, done)); result("HABITACION archivada=" + roomId); }
            } finally { ins.runOnMainSync(() -> SessionRepository.cerrar(context)); }
        }
    }
    private static String safe(Throwable error) {
        // Los mensajes de SDK no contienen contraseñas; no imprimir argumentos ni tokens.
        String message = error.getMessage(); return message == null ? "Sin detalle" : message;
    }
}
