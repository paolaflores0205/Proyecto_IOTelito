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
import com.example.proyecto_iotelito.data.taxiapi.TaxiApis;
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
        boolean onlySuperadmin = "true".equals(args.getString("qaOnlySuperadmin"));
        int failures = 0;
        for (UserRole role : UserRole.values()) {
            if (onlySuperadmin && role != UserRole.SUPERADMIN) continue;
            if (excludeSuperadmin && role == UserRole.SUPERADMIN) { result("SUPERADMIN pendiente_credencial_valida"); continue; }
            try {
                AuthenticatedUser user = login(role);
                if (role == UserRole.SUPERADMIN) {
                    QuerySnapshot users = Tasks.await(db.collection("users").get(Source.SERVER), 30, TimeUnit.SECONDS);
                    QuerySnapshot hotels = Tasks.await(db.collection("hotels").get(Source.SERVER), 30, TimeUnit.SECONDS);
                    result("SUPERADMIN usuarios=" + users.size() + " hoteles=" + hotels.size());
                    if (onlySuperadmin) {
                        QuerySnapshot reservations = Tasks.await(db.collection("reservations").get(Source.SERVER), 30, TimeUnit.SECONDS);
                        QuerySnapshot logs = Tasks.await(db.collection("logs").orderBy("createdAt", Query.Direction.DESCENDING).limit(200)
                                .get(Source.SERVER), 30, TimeUnit.SECONDS);
                        result("SUPERADMIN reservas=" + reservations.size() + " eventos=" + logs.size());
                        String qaReservation = args.getString("qaReservationId");
                        if (qaReservation != null) {
                            DocumentSnapshot qa = get("reservations/" + qaReservation);
                            assertTrue("Reserva QA visible", qa.exists());
                            int hotelId = HotelFirestoreCodec.integer(qa.get("hotelId"));
                            DocumentSnapshot hotelDoc = get("hotels/" + hotelId);
                            com.example.proyecto_iotelito.model.superadmin.ManagedHotel managed =
                                    HotelFirestoreCodec.decode(hotelDoc.getId(), hotelDoc.getData(), R.drawable.superadmin_hotel_hero);
                            Reserva booking = new Reserva(HotelFirestoreCodec.integer(qa.get("id")), hotelId,
                                    qa.getString("roomName"), qa.getString("roomNumber"), LocalDate.parse(qa.getString("fechaEntrada")),
                                    LocalDate.parse(qa.getString("fechaSalida")), qa.getString("huespedes"), qa.getDouble("precioTotal"),
                                    Reserva.Estado.desdeFirestore(qa.getString("estado")), qa.getString("codigo"));
                            for (com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.Period period :
                                    com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.Period.values()) {
                                com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.Result report =
                                        com.example.proyecto_iotelito.ui.superadmin.reportes.ReportCalculator.calculate(
                                                Collections.singletonList(managed), Collections.singletonList(booking), hotelId, period, booking.fechaEntrada);
                                assertEquals(1, report.bookings); assertEquals(175.75, report.amount, .001);
                                assertEquals(1, report.ranking.size()); result("SUPERADMIN reporte_QA_" + period + " correcto");
                            }
                        }
                        if ("true".equals(args.getString("qaScreens"))) {
                            com.example.proyecto_iotelito.SuperadminUiTest ui = new com.example.proyecto_iotelito.SuperadminUiTest();
                            ui.menuBlancoDe64dpMantieneOpcionesVisiblesYNavegacion();
                            result("SUPERADMIN menu_64dp_y_navegacion_usuarios_hoteles correctos");
                            ui.formularioValidaYAsignacionCargaAdministradoresReales();
                            result("SUPERADMIN formulario_asignacion_y_avisos correctos_sin_escrituras");
                        }
                    }
                } else if (role == UserRole.HOTEL_ADMIN) {
                    assertNotNull("Hotel asignado", user.getHotelId());
                    DocumentSnapshot hotel = get("hotels/" + user.getHotelId());
                    assertEquals(user.getUid(), hotel.getString("administratorUid"));
                    assertEquals(Boolean.TRUE, hotel.getBoolean("active"));
                    result("HOTEL_ADMIN hotel=" + user.getHotelId() + " asignacion_consistente");
                } else if (role == UserRole.TAXISTA) {
                    JSONObject driverProfile = TaxiApis.get(context).miPerfilTaxista();
                    assertTrue(driverProfile.getBoolean("activo"));
                    assertEquals("APROBADO",driverProfile.getString("estadoSolicitud"));
                    result(TaxiApis.fake()?"TAXISTA API_FAKE no_equivale_a_validacion_del_backend":"TAXISTA perfil_API_valido");
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

        // El recorrido taxi se prueba por separado en TaxiApiEmulatorTest.
        // Esta prueba real conserva únicamente las operaciones de hotel/reserva.
        AuthenticatedUser admin = login(UserRole.HOTEL_ADMIN);
        int hotelId = admin.getHotelId(); int roomId = FirebaseHotelAdminRepository.newId();
        int serviceId = FirebaseHotelAdminRepository.newId();
        String marker = "QA-" + Long.toString(System.currentTimeMillis(), 36).toUpperCase(Locale.ROOT);
        FirebaseHotelAdminRepository adminRepo = new FirebaseHotelAdminRepository(context);
        boolean roomCreated = false, serviceCreated = false;
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
                    175.75, marker, "Prueba QA", marker, Collections.singletonList(roomId), TarjetaSimulada.validar("4242424242424242", "12/39", "123", java.time.YearMonth.now()));
            assertTrue(get("reservations/" + booking.id).exists());
            result("RESERVA id=" + booking.id + " hotel=" + hotelId + " entrada=" + arrival + " codigo=" + marker);
            result("TAXI omitido: usar la prueba de API dedicada");
            if (!excludeSuperadmin) {
                login(UserRole.SUPERADMIN);
                QuerySnapshot reservations = Tasks.await(db.collection("reservations").get(Source.SERVER), 30, TimeUnit.SECONDS);
                assertTrue(reservations.getDocuments().stream().anyMatch(doc -> doc.getId().equals(String.valueOf(booking.id))));
                result("SUPERADMIN reserva_visible_para_reportes");
            }
        } finally {
            try {
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
