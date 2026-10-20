package com.example.proyecto_iotelito.data.superadmin;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.database.Cursor;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.data.HotelCatalogRepository;
import com.example.proyecto_iotelito.data.superadmin.local.*;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.example.proyecto_iotelito.notification.SuperadminNotifications;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.Assert.*;

/** Base temporal de pruebas: nunca borra ni reinicia iotelito_superadmin.db. */
@RunWith(AndroidJUnit4.class)
public class SuperadminStorageTest {
    private Context context;
    private String databaseName;
    private SuperadminDatabase db;
    private SuperadminRepository repository;
    private ExecutorService executor;

    @Before public void before() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        databaseName = "superadmin-test-" + UUID.randomUUID() + ".db";
        open();
    }

    private void open() throws Exception {
        db = Room.databaseBuilder(context, SuperadminDatabase.class, databaseName).build();
        executor = Executors.newSingleThreadExecutor();
        repository = new SuperadminRepository(context, db, executor);
        executor.submit(() -> { }).get(10, TimeUnit.SECONDS);
    }

    private void close() throws Exception {
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        db.close();
    }

    @After public void after() throws Exception {
        close();
        context.deleteDatabase(databaseName);
    }

    private interface Operation { void run(SuperadminRepository.Callback callback); }
    private int success(Operation operation) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        int[] id = {0};
        String[] error = {null};
        operation.run((value, message) -> { id[0] = value; error[0] = message; latch.countDown(); });
        assertTrue("La operación no terminó", latch.await(10, TimeUnit.SECONDS));
        assertNull(error[0]);
        return id[0];
    }

    private ManagedHotel newHotel(String name) {
        ManagedHotel hotel = new ManagedHotel(0, name, "Av. Prueba 123", "Miraflores", "Sin asignar",
                0, 0, R.drawable.superadmin_hotel_hero, true);
        hotel.city = "Lima";
        hotel.phone = "987654321";
        hotel.email = "prueba@example.com";
        hotel.description = "Hotel de prueba local";
        hotel.latitude = -12.12;
        hotel.longitude = -77.03;
        return hotel;
    }

    private ManagedHotel loadProfile(SuperadminRepository.HotelDetails legacy) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ManagedHotel[] hotel = {null};
        String[] error = {null};
        repository.loadHotelDetailsForAdmin(1, legacy, (value, message) -> {
            hotel[0] = value; error[0] = message; latch.countDown();
        });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNull(error[0]);
        return hotel[0];
    }

    @Test public void perfilAdminUsaLaBaseCompartidaSinPisarCambiosDeSuperadmin() throws Exception {
        AdminHotelEntity before = db.dao().hotel(1);
        before.name = "Nombre de superadmin";
        before.email = "superadmin@example.com";
        before.latitude = -12.12;
        before.longitude = -77.03;
        db.dao().updateHotel(before);

        SuperadminRepository.HotelDetails legacy = new SuperadminRepository.HotelDetails(
                "Nombre antiguo de admin", "Av. Admin 321", "Descripción antigua",
                "Parque cercano", "999888777", "admin@example.com");
        ManagedHotel migrated = loadProfile(legacy);
        assertEquals("Nombre de superadmin", migrated.name);
        assertEquals("superadmin@example.com", migrated.email);
        assertEquals("Av. Admin 321", migrated.address);
        assertEquals("Descripción antigua", migrated.description);
        assertEquals("Parque cercano", migrated.attractions);
        assertEquals("999888777", migrated.phone);

        SuperadminRepository.HotelDetails edited = new SuperadminRepository.HotelDetails(
                "Hotel actualizado por admin", "Av. Compartida 123", "Descripción compartida",
                "Museo cercano", "912345678", "contacto@example.com");
        success(callback -> repository.updateHotelDetailsFromAdmin(1, edited, callback));
        AdminHotelEntity saved = db.dao().hotel(1);
        assertEquals("Hotel actualizado por admin", saved.name);
        assertEquals("Museo cercano", saved.attractions);
        assertEquals(-12.12, saved.latitude, 0.00001);
        assertEquals(-77.03, saved.longitude, 0.00001);
        assertEquals(before.administratorId, saved.administratorId);
        assertEquals("Hotel actualizado por admin", HotelCatalogRepository.combinar(saved.toModel()).name);
        assertEquals("Descripción compartida", HotelCatalogRepository.combinar(saved.toModel()).description);
        assertEquals("Museo cercano", HotelCatalogRepository.combinar(saved.toModel()).attractions[0].name);
        close();
        open();
        assertEquals("Hotel actualizado por admin", db.dao().hotel(1).name);
        assertEquals("Museo cercano", db.dao().hotel(1).attractions);
        success(callback -> repository.updateHotelDetailsFromAdmin(1,
                new SuperadminRepository.HotelDetails(edited.name, edited.address,
                        edited.description, "", edited.phone, edited.email), callback));
        assertEquals(0, HotelCatalogRepository.combinar(db.dao().hotel(1).toModel()).attractions.length);
    }

    @Test public void migracionCuatroACincoConservaHotelYAgregaAtracciones() throws Exception {
        String name = "profile-migration-test-" + UUID.randomUUID() + ".db";
        SuperadminDatabase isolated = Room.databaseBuilder(context, SuperadminDatabase.class, name).build();
        try {
            AdminHotelEntity hotel = new AdminHotelEntity();
            hotel.id = 77;
            hotel.name = "Hotel conservado";
            hotel.photosJson = "[\"file:///foto.jpg\"]";
            isolated.dao().insertHotel(hotel);
            isolated.close();
            try (android.database.sqlite.SQLiteDatabase old = android.database.sqlite.SQLiteDatabase.openDatabase(
                    context.getDatabasePath(name).getPath(), null,
                    android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)) {
                old.execSQL("ALTER TABLE admin_hotels DROP COLUMN attractions");
                old.setVersion(4);
            }
            isolated = Room.databaseBuilder(context, SuperadminDatabase.class, name)
                    .addMigrations(SuperadminDatabase.MIGRATION_4_5).build();
            assertEquals("Hotel conservado", isolated.dao().hotel(77).name);
            assertEquals("file:///foto.jpg", isolated.dao().hotel(77).toModel().photos.get(0));
            assertNull(isolated.dao().hotel(77).attractions);
        } finally {
            if (isolated.isOpen()) isolated.close();
            context.deleteDatabase(name);
        }
    }

    private int count(String table) {
        try (Cursor cursor = db.getOpenHelper().getReadableDatabase().query("SELECT COUNT(*) FROM " + table)) {
            assertTrue(cursor.moveToFirst());
            return cursor.getInt(0);
        }
    }

    @Test public void conservaCatalogoYEstadoAlReabrirSinVolverASembrar() throws Exception {
        assertEquals(16, db.dao().countUsers());
        assertEquals(12, db.dao().countHotels());
        for (AdminHotelEntity row : db.dao().hotels()) assertEquals(SampleData.findById(row.id).name, row.name);
        success(callback -> repository.setUserActive(4, true, "Reactivación de prueba", callback));
        close();
        open();
        assertEquals(16, db.dao().countUsers());
        assertEquals(12, db.dao().countHotels());
        assertTrue(db.dao().user(4).active);
        assertEquals(2, count("admin_events"));
    }

    @Test public void registraEditaAsignaYConservaCoordenadasYEstado() throws Exception {
        int id = success(callback -> repository.saveHotel(newHotel("Hotel nuevo"), callback));
        ManagedHotel edit = db.dao().hotel(id).toModel();
        edit.attractions = "Museo del barrio";
        db.dao().updateHotel(AdminHotelEntity.from(edit));
        ManagedHotel renamed = new ManagedHotel(id, "Hotel editado", edit.address, edit.district,
                edit.administrator, edit.rooms, edit.reservations, edit.imageRes, true);
        renamed.city = edit.city;
        renamed.phone = edit.phone;
        renamed.email = edit.email;
        renamed.description = edit.description;
        renamed.latitude = -12.13;
        renamed.longitude = -77.04;
        success(callback -> repository.saveHotel(renamed, callback));
        success(callback -> repository.assignAdministrator(id, 5, callback));
        success(callback -> repository.setHotelActive(id, false, callback));
        close();
        open();
        AdminHotelEntity saved = db.dao().hotel(id);
        assertEquals(13, db.dao().countHotels());
        assertEquals("Hotel editado", saved.name);
        assertEquals(Integer.valueOf(5), saved.administratorId);
        assertEquals(db.dao().user(5).name, saved.administrator);
        assertEquals(-12.13, saved.latitude, 0.00001);
        assertEquals(-77.04, saved.longitude, 0.00001);
        assertEquals("prueba@example.com", saved.email);
        assertEquals("Museo del barrio", saved.attractions);
        assertFalse(saved.active);
    }

    @Test public void alertasNoSeDuplicanYSeRetiranAlAsignar() throws Exception {
        int initial = count("admin_alerts");
        assertEquals(3, initial);
        int id = success(callback -> repository.saveHotel(newHotel("Pendiente"), callback));
        repository.publishPendingAlerts();
        repository.publishPendingAlerts();
        executor.submit(() -> { }).get(10, TimeUnit.SECONDS);
        assertEquals(initial + 1, count("admin_alerts"));
        success(callback -> repository.assignAdministrator(id, 5, callback));
        assertEquals(initial, count("admin_alerts"));
    }

    @Test public void administradorInactivoNoModificaHotelNiGeneraEventoFalso() throws Exception {
        int eventsBefore = count("admin_events");
        Integer original = db.dao().hotel(1).administratorId;
        CountDownLatch latch = new CountDownLatch(1);
        String[] error = {null};
        repository.assignAdministrator(1, 14, (id, message) -> { error[0] = message; latch.countDown(); });
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertNotNull(error[0]);
        assertEquals(original, db.dao().hotel(1).administratorId);
        assertEquals(eventsBefore, count("admin_events"));
    }

    @Test public void notificacionUsaCanalAdministrativoYDestinoConIdentificador() {
        InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .grantRuntimePermission(context.getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        AdminAlertEntity alert = new AdminAlertEntity();
        alert.key = "test_notification_" + UUID.randomUUID();
        alert.hotelId = 1;
        alert.title = "Prueba de aviso administrativo";
        alert.message = "Toca para revisar el hotel";
        try {
            assertTrue(SuperadminNotifications.publish(context, alert));
            boolean found = false;
            // Android publica en otro proceso: notify() puede volver antes de que
            // el aviso aparezca en getActiveNotifications(). Esperamos la condición.
            long deadline = android.os.SystemClock.uptimeMillis() + 5000;
            while (!found && android.os.SystemClock.uptimeMillis() < deadline) {
                for (android.service.notification.StatusBarNotification item : context.getSystemService(NotificationManager.class).getActiveNotifications()) {
                    if (alert.title.equals(item.getNotification().extras.getString("android.title"))) {
                        found = true;
                        assertEquals(SuperadminNotifications.CHANNEL, item.getNotification().getChannelId());
                        assertNotNull(item.getNotification().contentIntent);
                    }
                }
                if (!found) android.os.SystemClock.sleep(30);
            }
            assertTrue("No se publicó la notificación", found);
        } finally {
            SuperadminNotifications.cancel(context, alert.key);
        }
    }
}
