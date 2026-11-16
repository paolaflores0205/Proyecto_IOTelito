package com.example.proyecto_iotelito.data;

import android.app.Activity;
import android.content.*;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.lifecycle.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.superadmin.*;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.auth.*;
import com.example.proyecto_iotelito.model.superadmin.HotelCheckout;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminMainActivity;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

/** Optativa: solo AVD y emuladores locales; jamás usa cuentas ni escrituras de Firebase real. */
@RunWith(AndroidJUnit4.class)
public class SuperadminReportsEmulatorTest {
    private final android.app.Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    private final Context context = ins.getTargetContext();
    private FirebaseOptions original;
    private FirebaseFirestore db;
    private Activity screen;
    private LiveData<FirebaseUsersRepository.State> users;
    private LiveData<FirebaseHotelsRepository.State> hotels;
    private LiveData<FirebaseAdminReadRepository.State<Reserva>> bookings;
    private LiveData<FirebaseAdminReadRepository.State<HotelCheckout>> checkouts;
    private final Observer<FirebaseUsersRepository.State> userObserver = value -> { };
    private final Observer<FirebaseHotelsRepository.State> hotelObserver = value -> { };
    private final Observer<FirebaseAdminReadRepository.State<Reserva>> bookingObserver = value -> { };
    private final Observer<FirebaseAdminReadRepository.State<HotelCheckout>> checkoutObserver = value -> { };

    private void waitFor(BooleanSupplier ready) {
        long end = SystemClock.uptimeMillis() + 25000;
        AtomicBoolean done = new AtomicBoolean();
        while (!done.get() && SystemClock.uptimeMillis() < end) {
            ins.runOnMainSync(() -> done.set(ready.getAsBoolean()));
            if (!done.get()) SystemClock.sleep(100);
        }
        assertTrue("La consulta/pantalla no completó la carga", done.get());
    }
    private void localRequest(String method, String url, String json) throws Exception {
        HttpURLConnection request = (HttpURLConnection) new URL(url).openConnection();
        request.setRequestMethod(method); request.setConnectTimeout(10000); request.setReadTimeout(10000);
        request.setRequestProperty("Authorization", "Bearer owner");
        if (json != null) {
            request.setRequestProperty("Content-Type", "application/json"); request.setDoOutput(true);
            try (java.io.OutputStream out = request.getOutputStream()) { out.write(json.getBytes(StandardCharsets.UTF_8)); }
        }
        assertEquals("Preparación del emulador local", 200, request.getResponseCode()); request.disconnect();
    }
    private void seed(String path, Object... pairs) throws Exception {
        JSONObject fields = new JSONObject();
        for (int i = 0; i < pairs.length; i += 2) {
            Object value = pairs[i + 1]; JSONObject field = new JSONObject();
            if (value == null) field.put("nullValue", JSONObject.NULL);
            else if (value instanceof Boolean) field.put("booleanValue", value);
            else if (value instanceof Number) field.put("integerValue", value.toString());
            else if (value instanceof Instant) field.put("timestampValue", value.toString());
            else if (value instanceof int[]) {
                JSONArray ids = new JSONArray();
                for (int id : (int[]) value) ids.put(new JSONObject().put("integerValue", String.valueOf(id)));
                field.put("arrayValue", new JSONObject().put("values", ids));
            } else field.put("stringValue", value);
            fields.put((String) pairs[i], field);
        }
        JSONObject document = new JSONObject().put("name", "projects/demo-iotelito-auth/databases/(default)/documents/" + path).put("fields", fields);
        localRequest("POST", "http://127.0.0.1:8180/v1/projects/demo-iotelito-auth/databases/(default)/documents:commit",
                new JSONObject().put("writes", new JSONArray().put(new JSONObject().put("update", document))).toString());
    }
    private void reservation(int id, String state, LocalDate start, LocalDate end, int amount, int... rooms) throws Exception {
        seed("reservations/" + id, "id", id, "hotelId", 1, "fechaEntrada", start.toString(), "fechaSalida", end.toString(),
                "precioTotal", amount, "estado", state, "roomIds", rooms, "roomName", "Suite", "roomNumber", "310",
                "huespedes", "2 adultos", "codigo", "LOCAL-" + id, "clientUid", "client");
    }
    private void screenshot(String name) throws Exception {
        ins.waitForIdleSync(); SystemClock.sleep(300);
        android.graphics.Bitmap bitmap = ins.getUiAutomation().takeScreenshot();
        assertNotNull(bitmap);
        try (java.io.FileOutputStream stream = new java.io.FileOutputStream(
                new java.io.File(context.getExternalFilesDir(null), name + ".png"))) {
            assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream));
        } finally { bitmap.recycle(); }
    }
    @After public void cleanup() throws Exception {
        if (original == null) return;
        ins.runOnMainSync(() -> {
            if (screen != null) screen.finish();
            if (users != null) users.removeObserver(userObserver);
            if (hotels != null) hotels.removeObserver(hotelObserver);
            if (bookings != null) bookings.removeObserver(bookingObserver);
            if (checkouts != null) checkouts.removeObserver(checkoutObserver);
            SessionRepository.cerrar(context);
        });
        Tasks.await(db.terminate(), 10, TimeUnit.SECONDS);
        FirebaseApp.getInstance().delete(); FirebaseApp.initializeApp(context, original);
    }
    @Test public void reportesYCargaConservanDatosConfirmadosSinConfundirCacheConError() throws Exception {
        assumeTrue("true".equals(InstrumentationRegistry.getArguments().getString("superadminEmulator")));
        original = FirebaseApp.getInstance().getOptions(); FirebaseApp.getInstance().delete();
        FirebaseApp.initializeApp(context, new FirebaseOptions.Builder().setApiKey("fake-api-key")
                .setApplicationId("1:123456:android:abcdef").setProjectId("demo-iotelito-auth").build());
        FirebaseAuth auth = FirebaseAuth.getInstance(); auth.useEmulator("127.0.0.1", 9099);
        db = FirebaseFirestore.getInstance(); db.useEmulator("127.0.0.1", 8180);
        db.setFirestoreSettings(new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build());
        localRequest("DELETE", "http://127.0.0.1:8180/emulator/v1/projects/demo-iotelito-auth/databases/(default)/documents", null);
        String email = "superadmin-" + System.currentTimeMillis() + "@example.test";
        String uid = Tasks.await(auth.createUserWithEmailAndPassword(email, "Local-test-123"), 20, TimeUnit.SECONDS).getUser().getUid();
        seed("users/" + uid, "uid", uid, "name", "Superadmin local", "email", email, "role", "SUPERADMIN", "active", true);
        seed("users/client", "uid", "client", "name", "Cliente local", "email", "client@example.test", "role", "CLIENT", "active", true);
        for (int id = 1; id <= 2; id++) seed("hotels/" + id, "id", id, "name", "Hotel QA " + id, "address", "Dirección QA",
                "city", "Lima", "district", "Prueba", "active", true, "rooms", 10, "hasInventory", true);
        LocalDate today = LocalDate.now(ZoneId.of("America/Lima"));
        reservation(101, "ACTIVA", today, today.plusDays(1), 100, 101, 102);
        reservation(102, "EN_CHECKOUT", today, today.plusDays(1), 200, 103);
        reservation(103, "FINALIZADA", today.minusDays(1), today, 300, 104);
        reservation(104, "COMPLETADA", today.minusDays(1), today, 400, 105);
        reservation(105, "CANCELADA", today, today.plusDays(1), 900, 106);
        seed("hotelCheckouts/103", "reservationId", "103", "hotelId", 1, "base", 300, "extra", 25, "total", 325,
                "createdAt", Instant.now(), "paymentMode", "SIMULATED", "clientUid", "client");
        ins.runOnMainSync(() -> {
            SessionRepository.guardar(context, new AuthenticatedUser(email, "Superadmin local", UserRole.SUPERADMIN, uid));
            users = new FirebaseUsersRepository().observeUsers();
            hotels = new FirebaseHotelsRepository(context, false).observe();
            FirebaseAdminReadRepository repository = new FirebaseAdminReadRepository();
            bookings = repository.reservations(); checkouts = repository.checkouts();
            users.observeForever(userObserver); hotels.observeForever(hotelObserver);
            bookings.observeForever(bookingObserver); checkouts.observeForever(checkoutObserver);
        });
        waitFor(() -> users.getValue() != null && !users.getValue().loading && hotels.getValue() != null
                && !hotels.getValue().loading && bookings.getValue() != null && !bookings.getValue().loading
                && checkouts.getValue() != null && !checkouts.getValue().loading);
        assertNull(users.getValue().error); assertNull(hotels.getValue().error);
        assertNull(bookings.getValue().error); assertNull(checkouts.getValue().error);
        assertEquals(5, bookings.getValue().rows.size()); assertEquals(1, checkouts.getValue().rows.size());
        Tasks.await(db.disableNetwork(), 10, TimeUnit.SECONDS);
        waitFor(() -> users.getValue().loading && hotels.getValue().loading && bookings.getValue().loading);
        assertNull(users.getValue().error); assertEquals(2, users.getValue().users.size());
        assertNull(hotels.getValue().error); assertEquals(2, hotels.getValue().hotels.size());
        assertEquals(5, bookings.getValue().rows.size());
        Tasks.await(db.enableNetwork(), 10, TimeUnit.SECONDS);
        waitFor(() -> !users.getValue().loading && !hotels.getValue().loading && !bookings.getValue().loading);
        ins.getUiAutomation().grantRuntimePermission(context.getPackageName(), android.Manifest.permission.POST_NOTIFICATIONS);
        screen = ins.startActivitySync(new Intent(context, SuperadminMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.runOnMainSync(() -> ((SuperadminMainActivity) screen).openTab(R.id.nav_superadmin_reports));
        waitFor(() -> screen.findViewById(R.id.tv_checkout_income) != null
                && ((TextView) screen.findViewById(R.id.tv_checkout_income)).getText().toString().contains("325"));
        ins.runOnMainSync(() -> screen.findViewById(R.id.chip_daily).performClick());
        waitFor(() -> ((TextView) screen.findViewById(R.id.tv_income)).getText().toString().contains("300"));
        assertEquals("3", ((TextView) screen.findViewById(R.id.tv_bookings)).getText().toString());
        screenshot("superadmin-reportes");
        ins.runOnMainSync(() -> {
            android.view.View current = screen.findViewById(R.id.tv_checkout_status);
            android.view.ViewParent parent = current.getParent();
            while (parent != null && !(parent instanceof androidx.core.widget.NestedScrollView)) parent = parent.getParent();
            if (parent instanceof androidx.core.widget.NestedScrollView)
                ((androidx.core.widget.NestedScrollView) parent).scrollTo(0, current.getTop());
        });
        screenshot("superadmin-reportes-grafica");
        ins.runOnMainSync(() -> {
            android.widget.AutoCompleteTextView selector = screen.findViewById(R.id.actv_hotel_selector);
            selector.setText(selector.getAdapter().getItem(2).toString(), false);
            selector.getOnItemClickListener().onItemClick(null, null, 2, 2);
        });
        waitFor(() -> ((TextView) screen.findViewById(R.id.tv_bookings)).getText().toString().equals("0"));
        assertTrue(((TextView) screen.findViewById(R.id.tv_checkout_income)).getText().toString().contains("0"));
    }
}
