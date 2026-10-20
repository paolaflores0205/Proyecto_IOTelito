package com.example.proyecto_iotelito.data.superadmin;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.data.SampleHotelGalleries;
import com.example.proyecto_iotelito.data.superadmin.local.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SampleHotelGalleriesTest {
    private final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    private String name;
    private SuperadminDatabase db;
    private ExecutorService worker;
    @Before public void before() {
        name = "sample-photos-test-" + UUID.randomUUID() + ".db";
        db = Room.databaseBuilder(context, SuperadminDatabase.class, name).build();
        worker = Executors.newSingleThreadExecutor();
    }
    private void seed() throws Exception {
        new SuperadminRepository(context, db, worker);
        worker.submit(() -> {}).get(15, TimeUnit.SECONDS);
    }
    @After public void after() throws Exception {
        worker.shutdown();
        worker.awaitTermination(15, TimeUnit.SECONDS);
        db.close();
        context.deleteDatabase(name);
    }
    @Test public void doceHotelesTienenCuatroFotosLocalesQueSePuedenAbrir() throws Exception {
        seed();
        assertEquals(12, db.dao().countHotels());
        Set<String> distinct = new HashSet<>();
        for (AdminHotelEntity row : db.dao().hotels()) {
            assertTrue(row.galleryInitialized);
            List<String> photos = row.toModel().photos;
            assertEquals(4, photos.size());
            assertEquals(4, new HashSet<>(photos).size());
            for (String photo : photos) {
                assertTrue(SampleHotelGalleries.isIllustrative(photo));
                distinct.add(photo);
            }
        }
        for (String photo : distinct) {
            try (InputStream input = context.getContentResolver().openInputStream(Uri.parse(photo))) {
                BitmapFactory.Options bounds = new BitmapFactory.Options();
                bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(input, null, bounds);
                assertEquals(960, bounds.outWidth);
                assertEquals(640, bounds.outHeight);
            }
        }
        seed();
        for (AdminHotelEntity row : db.dao().hotels()) assertEquals(4, row.toModel().photos.size());
        assertTrue(SampleHotelGalleries.forHotel(44).isEmpty());
    }
    @Test public void conservaFotosPropiasYNoReponeUnaGaleriaVaciadaPorElUsuario() throws Exception {
        AdminHotelEntity hotel = new AdminHotelEntity();
        hotel.id = 1; hotel.name = "Hotel editado"; hotel.photosJson = "[\"file:///foto-propia.jpg\"]";
        db.dao().insertHotel(hotel);
        seed();
        AdminHotelEntity preserved = db.dao().hotel(1);
        assertEquals("file:///foto-propia.jpg", preserved.toModel().photos.get(0));
        assertTrue(preserved.galleryInitialized);
        preserved.photosJson = "[]";
        db.dao().updateHotel(preserved);
        seed();
        assertTrue(db.dao().hotel(1).toModel().photos.isEmpty());
    }
    @Test public void migracionDesdeTresConservaLasFotosYaGuardadas() throws Exception {
        AdminHotelEntity hotel = new AdminHotelEntity();
        hotel.id = 1; hotel.name = "Hotel existente"; hotel.photosJson = "[\"file:///foto-guardada.jpg\"]";
        db.dao().insertHotel(hotel);
        db.close();
        try (android.database.sqlite.SQLiteDatabase old = android.database.sqlite.SQLiteDatabase.openDatabase(
                context.getDatabasePath(name).getPath(), null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)) {
            old.execSQL("ALTER TABLE admin_hotels DROP COLUMN galleryInitialized");
            old.execSQL("ALTER TABLE admin_hotels DROP COLUMN attractions");
            old.setVersion(3);
        }
        db = Room.databaseBuilder(context, SuperadminDatabase.class, name)
                .addMigrations(SuperadminDatabase.MIGRATION_3_4, SuperadminDatabase.MIGRATION_4_5).build();
        assertEquals("file:///foto-guardada.jpg", db.dao().hotel(1).toModel().photos.get(0));
        assertFalse(db.dao().hotel(1).galleryInitialized);
        seed();
        assertEquals("file:///foto-guardada.jpg", db.dao().hotel(1).toModel().photos.get(0));
        assertTrue(db.dao().hotel(1).galleryInitialized);
    }
}
