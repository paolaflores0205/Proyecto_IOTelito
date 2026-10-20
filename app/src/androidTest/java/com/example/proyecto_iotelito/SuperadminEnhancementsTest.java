package com.example.proyecto_iotelito;

import android.app.*;
import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.*;
import android.net.Uri;
import android.os.SystemClock;
import android.widget.*;
import androidx.room.Room;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.data.HotelPhotoStore;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.data.superadmin.local.*;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminMainActivity;
import com.example.proyecto_iotelito.ui.superadmin.hoteles.*;
import com.google.android.material.chip.Chip;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SuperadminEnhancementsTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    private final Context context = ins.getTargetContext();
    private void await(BooleanSupplier condition) {
        long end = SystemClock.uptimeMillis() + 12000;
        AtomicBoolean done = new AtomicBoolean();
        while (!done.get() && SystemClock.uptimeMillis() < end) {
            ins.runOnMainSync(() -> done.set(condition.getAsBoolean()));
            if (!done.get()) SystemClock.sleep(50);
        }
        assertTrue("La pantalla no se actualizó", done.get());
        ins.waitForIdleSync();
    }
    private Activity open(Class<? extends Activity> type) {
        return ins.startActivitySync(new Intent(context, type).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }
    @Test public void fotoDeReferenciaQuedaCentradaAlRegistrarYEditar() {
        for (int hotelId : new int[]{0, 1}) {
            Activity form = ins.startActivitySync(new Intent(context, FormularioHotelActivity.class)
                    .putExtra(FormularioHotelActivity.EXTRA_HOTEL_ID, hotelId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            try {
                await(() -> form.findViewById(R.id.btn_save).isEnabled());
                ins.runOnMainSync(() -> ((HotelGalleryView) form.findViewById(R.id.photo_gallery))
                        .show(Collections.emptyList(), R.drawable.superadmin_hotel_hero, null));
                await(() -> {
                    HotelGalleryView gallery = form.findViewById(R.id.photo_gallery);
                    LinearLayout row = (LinearLayout) gallery.getChildAt(0);
                    if (row.getChildCount() != 1 || row.getWidth() == 0) return false;
                    android.view.View card = row.getChildAt(0);
                    int left = card.getLeft(), right = row.getWidth() - card.getRight();
                    return Math.abs(left - right) <= 1;
                });
            } finally { ins.runOnMainSync(form::finish); }
        }
    }
    @Test public void alertasAbrenListasFiltradasYReportesCambianTituloYRanking() throws Exception {
        SuperadminRepository.getInstance(context).awaitInitialization();
        int unassigned = 0, inactive = 0;
        for (AdminHotelEntity hotel : SuperadminDatabase.getInstance(context).dao().hotels()) if (hotel.administratorId == null) unassigned++;
        // La lista de usuarios se consulta con SQL en el hilo de pruebas, no en UI.
        try (android.database.Cursor cursor = SuperadminDatabase.getInstance(context).getOpenHelper().getReadableDatabase()
                .query("SELECT COUNT(*) FROM admin_users WHERE active=0")) { cursor.moveToFirst(); inactive = cursor.getInt(0); }
        final int expectedHotels = unassigned, expectedUsers = inactive;
        Activity activity = open(SuperadminMainActivity.class);
        try {
            await(() -> activity.findViewById(R.id.card_unassigned_alert) != null);
            ins.runOnMainSync(() -> activity.findViewById(R.id.card_unassigned_alert).performClick());
            await(() -> activity.findViewById(R.id.rv_hotels) != null &&
                    ((RecyclerView) activity.findViewById(R.id.rv_hotels)).getAdapter().getItemCount() == expectedHotels);
            ins.runOnMainSync(() -> {
                assertTrue(((Chip) activity.findViewById(R.id.chip_unassigned)).isChecked());
                activity.findViewById(R.id.nav_superadmin_home_button).performClick();
            });
            await(() -> activity.findViewById(R.id.card_inactive_alert) != null);
            ins.runOnMainSync(() -> activity.findViewById(R.id.card_inactive_alert).performClick());
            await(() -> activity.findViewById(R.id.rv_users) != null &&
                    ((RecyclerView) activity.findViewById(R.id.rv_users)).getAdapter().getItemCount() == expectedUsers);
            ins.runOnMainSync(() -> {
                assertTrue(((Chip) activity.findViewById(R.id.chip_inactive)).isChecked());
                activity.findViewById(R.id.nav_superadmin_reports_button).performClick();
            });
            await(() -> activity.findViewById(R.id.ranking_container) != null &&
                    ((LinearLayout) activity.findViewById(R.id.ranking_container)).getChildCount() >= 24);
            ins.runOnMainSync(() -> {
                ((Chip) activity.findViewById(R.id.chip_daily)).setChecked(true);
                assertEquals("Reservas del día", ((TextView) activity.findViewById(R.id.tv_chart_title)).getText().toString());
                ((Chip) activity.findViewById(R.id.chip_yearly)).setChecked(true);
                assertEquals("Reservas del año", ((TextView) activity.findViewById(R.id.tv_chart_title)).getText().toString());
                AutoCompleteTextView selector = activity.findViewById(R.id.actv_hotel_selector);
                selector.setText(selector.getAdapter().getItem(1).toString(), false);
                selector.getOnItemClickListener().onItemClick(null, null, 1, 1);
                assertEquals(2, ((LinearLayout) activity.findViewById(R.id.ranking_container)).getChildCount());
            });
        } finally { ins.runOnMainSync(activity::finish); }
    }
    @Test public void fotosSeCopianPersistenYLaMigracionConservaElHotel() throws Exception {
        File source = new File(context.getCacheDir(), "photo-test-" + UUID.randomUUID() + ".png");
        String dbName = "gallery-test-" + UUID.randomUUID() + ".db";
        List<String> photos = new ArrayList<>();
        SuperadminDatabase db = null;
        Activity form = null;
        try {
            Bitmap bitmap = Bitmap.createBitmap(30, 20, Bitmap.Config.ARGB_8888);
            bitmap.eraseColor(Color.CYAN);
            try (OutputStream output = new FileOutputStream(source)) { bitmap.compress(Bitmap.CompressFormat.PNG, 100, output); }
            bitmap.recycle();
            photos = HotelPhotoStore.importPhotos(context, Arrays.asList(Uri.fromFile(source), Uri.fromFile(source)));
            assertEquals(2, photos.size());
            for (String photo : photos) assertTrue(new File(Uri.parse(photo).getPath()).isFile());
            ManagedHotel hotel = new ManagedHotel(77, "Fotos persistentes", "Calle", "Distrito", "Sin asignar", 0, 0, R.drawable.superadmin_hotel_hero, true);
            hotel.photos.addAll(photos);
            db = Room.databaseBuilder(context, SuperadminDatabase.class, dbName).addMigrations(SuperadminDatabase.MIGRATION_1_2, SuperadminDatabase.MIGRATION_2_3, SuperadminDatabase.MIGRATION_3_4, SuperadminDatabase.MIGRATION_4_5).build();
            db.dao().insertHotel(AdminHotelEntity.from(hotel));
            db.close();
            db = Room.databaseBuilder(context, SuperadminDatabase.class, dbName).addMigrations(SuperadminDatabase.MIGRATION_1_2, SuperadminDatabase.MIGRATION_2_3, SuperadminDatabase.MIGRATION_3_4, SuperadminDatabase.MIGRATION_4_5).build();
            assertEquals(photos, db.dao().hotel(77).toModel().photos);
            db.close();
            // Reconstruye la estructura de la versión anterior, solo en esta base temporal.
            try (SQLiteDatabase legacy = SQLiteDatabase.openDatabase(context.getDatabasePath(dbName).getPath(), null, SQLiteDatabase.OPEN_READWRITE)) {
                legacy.execSQL("ALTER TABLE admin_hotels DROP COLUMN photosJson");
                legacy.execSQL("ALTER TABLE admin_hotels DROP COLUMN galleryInitialized");
                legacy.execSQL("ALTER TABLE admin_hotels DROP COLUMN attractions");
                legacy.setVersion(1);
            }
            db = Room.databaseBuilder(context, SuperadminDatabase.class, dbName).addMigrations(SuperadminDatabase.MIGRATION_1_2, SuperadminDatabase.MIGRATION_2_3, SuperadminDatabase.MIGRATION_3_4, SuperadminDatabase.MIGRATION_4_5).build();
            assertEquals("Fotos persistentes", db.dao().hotel(77).name);
            assertTrue(db.dao().hotel(77).toModel().photos.isEmpty());
            assertEquals(5, db.getOpenHelper().getReadableDatabase().getVersion());
            form = open(FormularioHotelActivity.class);
            final Activity screen = form;
            final List<String> galleryPhotos = new ArrayList<>(photos);
            ins.runOnMainSync(() -> {
                HotelGalleryView gallery = screen.findViewById(R.id.photo_gallery);
                Runnable refresh = () -> gallery.show(galleryPhotos, R.drawable.superadmin_hotel_hero, null);
                gallery.show(galleryPhotos, R.drawable.superadmin_hotel_hero, refresh);
                LinearLayout row = (LinearLayout) gallery.getChildAt(0);
                assertEquals(2, row.getChildCount());
                LinearLayout first = (LinearLayout) row.getChildAt(0);
                LinearLayout actions = (LinearLayout) first.getChildAt(2);
                actions.getChildAt(1).performClick();
                assertEquals(1, galleryPhotos.size());
            });
        } finally {
            if (form != null) { final Activity screen = form; ins.runOnMainSync(screen::finish); }
            if (db != null && db.isOpen()) db.close();
            context.deleteDatabase(dbName);
            source.delete();
            for (String photo : photos) new File(Uri.parse(photo).getPath()).delete();
        }
    }
    @Test public void selectorDeMapaPermiteEscribirDireccionSinPedirCoordenadas() {
        Activity map = open(SeleccionarUbicacionActivity.class);
        try {
            ins.runOnMainSync(() -> {
                assertNotNull(map.findViewById(R.id.et_map_search));
                assertNotNull(map.findViewById(R.id.btn_search_address));
                assertFalse(map.findViewById(R.id.btn_confirm_location).isEnabled());
            });
        } finally { ins.runOnMainSync(map::finish); }
    }
}
