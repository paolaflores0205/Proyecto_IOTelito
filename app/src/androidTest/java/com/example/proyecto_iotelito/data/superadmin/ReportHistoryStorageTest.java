package com.example.proyecto_iotelito.data.superadmin;

import android.content.Context;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.data.local.IoTelitoDatabase;
import com.example.proyecto_iotelito.data.superadmin.local.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ReportHistoryStorageTest {
    @Test public void historialSeGuardaUnaVezConservaHotelesYNoModificaReservasDeCliente() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "report-history-test-" + UUID.randomUUID() + ".db";
        SuperadminDatabase db = Room.databaseBuilder(context, SuperadminDatabase.class, name).build();
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            int clientBefore = IoTelitoDatabase.getInstance(context).reservaDao().contar();
            SuperadminRepository repository = new SuperadminRepository(context, db, worker);
            worker.submit(() -> {}).get(15, TimeUnit.SECONDS);
            repository.reportHistory();
            worker.submit(() -> {}).get(30, TimeUnit.SECONDS);
            int initial = db.dao().countReportHistory();
            assertTrue(initial > 20000);
            repository.reportHistory();
            worker.submit(() -> {}).get(30, TimeUnit.SECONDS);
            assertEquals(initial, db.dao().countReportHistory());
            assertEquals(12, db.dao().countHotels());
            assertEquals(clientBefore, IoTelitoDatabase.getInstance(context).reservaDao().contar());
            db.close();
            db = Room.databaseBuilder(context, SuperadminDatabase.class, name).build();
            assertEquals(initial, db.dao().countReportHistory());
        } finally {
            worker.shutdown();
            worker.awaitTermination(15, TimeUnit.SECONDS);
            db.close();
            context.deleteDatabase(name);
        }
    }
    @Test public void migracionDesdeVersionDosConservaGaleriaYAgregaSoloTablaDeReportes() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name = "report-migration-test-" + UUID.randomUUID() + ".db";
        SuperadminDatabase db = Room.databaseBuilder(context, SuperadminDatabase.class, name).build();
        try {
            AdminHotelEntity hotel = new AdminHotelEntity();
            hotel.id = 88; hotel.name = "Hotel existente"; hotel.photosJson = "[\"file:///foto-existente.jpg\"]";
            db.dao().insertHotel(hotel);
            db.close();
            try (android.database.sqlite.SQLiteDatabase old = android.database.sqlite.SQLiteDatabase.openDatabase(
                    context.getDatabasePath(name).getPath(), null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)) {
                old.execSQL("DROP TABLE report_demo_bookings");
                old.execSQL("ALTER TABLE admin_hotels DROP COLUMN galleryInitialized");
                old.execSQL("ALTER TABLE admin_hotels DROP COLUMN attractions");
                old.setVersion(2);
            }
            db = Room.databaseBuilder(context, SuperadminDatabase.class, name)
                    .addMigrations(SuperadminDatabase.MIGRATION_2_3, SuperadminDatabase.MIGRATION_3_4,
                            SuperadminDatabase.MIGRATION_4_5).build();
            assertEquals("Hotel existente", db.dao().hotel(88).name);
            assertEquals("file:///foto-existente.jpg", db.dao().hotel(88).toModel().photos.get(0));
            assertEquals(0, db.dao().countReportHistory());
        } finally { db.close(); context.deleteDatabase(name); }
    }
}
