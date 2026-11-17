package com.example.proyecto_iotelito.data;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import android.app.*;
import android.content.*;
import android.os.*;
import android.view.*;
import android.widget.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.*;

import com.example.proyecto_iotelito.*;
import com.example.proyecto_iotelito.BuildConfig;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.taxiapi.*;
import com.example.proyecto_iotelito.model.auth.*;
import com.example.proyecto_iotelito.ui.taxi.*;
import com.example.proyecto_iotelito.ui.taxista.*;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;

import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;

/** Optativa: solo AVD temporal + Auth 9099 / Firestore 8180 del proyecto demo. */
@RunWith(AndroidJUnit4.class)
public class TaxiApiEmulatorTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    private final Context context = ins.getTargetContext();
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseOptions original;
    private String clientUid, driverUid;
    private final String password = "local-emulator-only";

    private Activity current() {
        AtomicReference<Activity> r = new AtomicReference<>();
        Runnable read =
                () -> {
                    for (Activity a :
                            ActivityLifecycleMonitorRegistry.getInstance()
                                    .getActivitiesInStage(Stage.RESUMED)) r.set(a);
                };
        if (Looper.myLooper() == Looper.getMainLooper()) read.run();
        else ins.runOnMainSync(read);
        return r.get();
    }

    private void waitFor(BooleanSupplier condition) {
        long end = SystemClock.uptimeMillis() + 30000;
        while (SystemClock.uptimeMillis() < end) {
            if (condition.getAsBoolean()) return;
            SystemClock.sleep(100);
        }
        try {
            screenshot("taxi-api-failure");
        } catch (Exception ignored) {
        }
        fail("No terminó la operación/pantalla: " + current());
    }

    private boolean text(int id, String part) {
        AtomicBoolean yes = new AtomicBoolean();
        ins.runOnMainSync(
                () -> {
                    Activity a = current();
                    View v = a == null ? null : a.findViewById(id);
                    yes.set(
                            v instanceof TextView
                                    && ((TextView) v).getText().toString().contains(part));
                });
        return yes.get();
    }

    private void click(int id) {
        waitFor(
                () -> {
                    Activity a = current();
                    if (a == null) return false;
                    AtomicBoolean ready = new AtomicBoolean();
                    ins.runOnMainSync(
                            () -> {
                                View v = a.findViewById(id);
                                ready.set(v != null && v.isEnabled());
                            });
                    return ready.get();
                });
        ins.runOnMainSync(
                () -> {
                    Activity a = current();
                    a.findViewById(id).performClick();
                });
    }

    private void open(Class<?> type, int id) {
        ins.startActivitySync(
                new Intent(context, type)
                        .putExtra(TaxiRepository.EXTRA_ID, id)
                        .putExtra(SolicitudTaxiActivity.EXTRA_RESERVA_ID, id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void closeAll() {
        ins.runOnMainSync(
                () -> {
                    for (Stage stage :
                            new Stage[] {
                                Stage.RESUMED,
                                Stage.STARTED,
                                Stage.CREATED,
                                Stage.STOPPED,
                                Stage.PAUSED
                            })
                        for (Activity a :
                                new ArrayList<>(
                                        ActivityLifecycleMonitorRegistry.getInstance()
                                                .getActivitiesInStage(stage))) a.finish();
                });
        ins.waitForIdleSync();
    }

    private void login(String email, UserRole role, String uid) throws Exception {
        closeAll();
        ins.runOnMainSync(
                () -> {
                    TaxistaLocationService.stop(context);
                    SessionRepository.cerrar(context);
                });
        Tasks.await(auth.signInWithEmailAndPassword(email, password), 20, TimeUnit.SECONDS);
        ins.runOnMainSync(
                () ->
                        SessionRepository.guardar(
                                context, new AuthenticatedUser(email, "Cuenta QA", role, uid)));
    }

    private void seed(String path, Object... pairs) throws Exception {
        JSONObject fields = new JSONObject();
        for (int i = 0; i < pairs.length; i += 2) {
            Object v = pairs[i + 1];
            JSONObject f = new JSONObject();
            if (v == null) f.put("nullValue", JSONObject.NULL);
            else if (v instanceof Boolean) f.put("booleanValue", v);
            else if (v instanceof Number) f.put("integerValue", v.toString());
            else f.put("stringValue", v);
            fields.put((String) pairs[i], f);
        }
        HttpURLConnection c =
                (HttpURLConnection)
                        new URL(
                                        "http://127.0.0.1:8180/v1/projects/demo-iotelito-auth/databases/(default)/documents:commit")
                                .openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(10000);
        c.setReadTimeout(10000);
        c.setRequestProperty("Authorization", "Bearer owner");
        c.setRequestProperty("Content-Type", "application/json");
        c.setDoOutput(true);
        JSONObject update =
                new JSONObject()
                        .put(
                                "name",
                                "projects/demo-iotelito-auth/databases/(default)/documents/" + path)
                        .put("fields", fields);
        try (java.io.OutputStream out = c.getOutputStream()) {
            out.write(
                    new JSONObject()
                            .put(
                                    "writes",
                                    new JSONArray().put(new JSONObject().put("update", update)))
                            .toString()
                            .getBytes(StandardCharsets.UTF_8));
        }
        assertEquals(200, c.getResponseCode());
        c.disconnect();
    }

    private void screenshot(String name) throws Exception {
        android.graphics.Bitmap bitmap = ins.getUiAutomation().takeScreenshot();
        try (java.io.FileOutputStream out =
                new java.io.FileOutputStream(
                        new java.io.File(context.getExternalFilesDir(null), name + ".png"))) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out);
        }
        bitmap.recycle();
    }

    @After
    public void cleanup() throws Exception {
        if (original == null) return;
        closeAll();
        ins.runOnMainSync(
                () -> {
                    TaxistaLocationService.stop(context);
                    SessionRepository.cerrar(context);
                });
        Tasks.await(db.terminate(), 10, TimeUnit.SECONDS);
        FirebaseApp.getInstance().delete();
        FirebaseApp.initializeApp(context, original);
    }

    @Test
    public void recorridoTaxiConApiFalsaYReservasLocalesEmuladas() throws Exception {
        assumeTrue(
                "true".equals(InstrumentationRegistry.getArguments().getString("taxiApiEmulator")));
        HttpURLConnection patch=(HttpURLConnection)new URL("https://example.invalid").openConnection();
        patch.setRequestMethod("PATCH");
        assertEquals("PATCH",patch.getRequestMethod());
        patch.disconnect(); // Verifica soporte Android sin enviar una petición de red.
        original = FirebaseApp.getInstance().getOptions();
        FirebaseApp.getInstance().delete();
        FirebaseApp.initializeApp(
                context,
                new FirebaseOptions.Builder()
                        .setProjectId("demo-iotelito-auth")
                        .setApiKey("fake-api-key")
                        .setApplicationId("1:123456:android:abcdef")
                        .build());
        auth = FirebaseAuth.getInstance();
        auth.useEmulator("127.0.0.1", 9099);
        db = FirebaseFirestore.getInstance();
        db.useEmulator("127.0.0.1", 8180);
        db.setFirestoreSettings(
                new FirebaseFirestoreSettings.Builder().setPersistenceEnabled(false).build());
        String suffix = String.valueOf(System.currentTimeMillis());
        String clientEmail = "client-" + suffix + "@example.test",
                driverEmail = "driver-" + suffix + "@example.test";
        clientUid =
                Tasks.await(
                                auth.createUserWithEmailAndPassword(clientEmail, password),
                                20,
                                TimeUnit.SECONDS)
                        .getUser()
                        .getUid();
        auth.signOut();
        driverUid =
                Tasks.await(
                                auth.createUserWithEmailAndPassword(driverEmail, password),
                                20,
                                TimeUnit.SECONDS)
                        .getUser()
                        .getUid();
        auth.signOut();
        seed(
                "users/" + clientUid,
                "uid",
                clientUid,
                "name",
                "Cliente QA",
                "email",
                clientEmail,
                "role",
                "CLIENT",
                "active",
                true);
        seed(
                "users/" + driverUid,
                "uid",
                driverUid,
                "name",
                "Taxista QA",
                "email",
                driverEmail,
                "role",
                "TAXISTA",
                "active",
                true);
        seed(
                "hotels/7",
                "id",
                7,
                "name",
                "Hotel QA",
                "address",
                "Av. de prueba 123",
                "city",
                "Lima",
                "district",
                "Prueba",
                "active",
                true,
                "hasInventory",
                false,
                "rooms",
                0,
                "pricePerNight",
                0);
        seed(
                "reservations/1001",
                "id",
                1001,
                "hotelId",
                7,
                "clientUid",
                clientUid,
                "roomName",
                "Doble",
                "roomNumber",
                "101",
                "fechaEntrada",
                "2026-11-15",
                "fechaSalida",
                "2026-11-16",
                "huespedes",
                "2 adultos",
                "precioTotal",
                1280,
                "estado",
                "EN_CHECKOUT",
                "codigo",
                "QA-TAXI");
        login(clientEmail, UserRole.CLIENT, clientUid);
        if (!BuildConfig.TAXI_API_FAKE) {
            open(SeguimientoTaxiActivity.class, 1001);
            waitFor(
                    () -> {
                        Activity a = current();
                        AtomicBoolean shown = new AtomicBoolean();
                        ins.runOnMainSync(
                                () -> {
                                    View v =
                                            a == null
                                                    ? null
                                                    : a.findViewById(android.R.id.content)
                                                            .findViewWithTag("taxi_api_status");
                                    shown.set(
                                            v instanceof TextView
                                                    && ((TextView) v)
                                                            .getText()
                                                            .toString()
                                                            .contains("no está disponible"));
                                });
                        return shown.get();
                    });
            assertNotNull(ReservaRepository.getInstance(context).buscarPorId(1001));
            screenshot("taxi-api-no-disponible");
            return;
        }
        open(SolicitudTaxiActivity.class, 1001);
        waitFor(() -> text(R.id.taxi_aeropuerto_selector, "Jorge Chávez"));
        screenshot("taxi-api-solicitud");
        click(R.id.btn_confirmar_solicitud);
        waitFor(() -> current() instanceof TaxistaAsignadoActivity);
        assertEquals("SOLICITADO", TaxiApis.get(context).verServicio(1001).optString("estado"));
        login(driverEmail, UserRole.TAXISTA, driverUid);
        TaxiApis.get(context).cambiarDisponibilidad(true);
        context.getSharedPreferences("taxi_location", 0).edit().putBoolean("asked", true).commit();
        open(DetalleServicioActivity.class, 1001);
        waitFor(() -> text(R.id.tv_pasajero_nombre, "QA"));
        waitFor(
                () -> {
                    AtomicBoolean v = new AtomicBoolean();
                    ins.runOnMainSync(
                            () ->
                                    v.set(
                                            com.example.proyecto_iotelito.data.taxista
                                                    .ConductorPreferencias.disponible(
                                                    context, driverEmail)));
                    return v.get();
                });
        click(R.id.btn_aceptar_servicio);
        waitFor(() -> current() instanceof ServicioAsignadoActivity);
        click(R.id.btn_iniciar_recorrido);
        waitFor(() -> current() instanceof DireccionHotelActivity);
        click(R.id.btn_llegue_al_hotel);
        waitFor(() -> current() instanceof DireccionAeropuertoActivity);
        assertEquals("EN_TRASLADO", TaxiApis.get(context).verServicio(1001).optString("estado"));
        // El AVD recibe geo fix desde adb; comprobar el envío real mediante FusedLocation.
        waitFor(() -> TaxiApis.get(context).verServicio(1001).optJSONObject("ubicacion") != null);
        login(clientEmail, UserRole.CLIENT, clientUid);
        open(SeguimientoTaxiActivity.class, 1001);
        waitFor(() -> text(R.id.taxi_map_info, "DEMO-001"));
        SystemClock.sleep(4000);
        screenshot("taxi-api-mapa");
        click(R.id.btn_ver_qr);
        waitFor(() -> current() instanceof CodigoQrTaxiActivity);
        waitFor(
                () -> {
                    AtomicBoolean shown = new AtomicBoolean();
                    ins.runOnMainSync(
                            () -> {
                                Activity a = current();
                                ImageView v = a == null ? null : a.findViewById(R.id.taxi_qr_image);
                                shown.set(v != null && v.getDrawable() != null);
                            });
                    return shown.get();
                });
        screenshot("taxi-api-qr");
        String qr = TaxiApis.get(context).verQr(1001).optString("codigo");
        AtomicReference<android.graphics.Bitmap> qrBitmap = new AtomicReference<>();
        ins.runOnMainSync(
                () ->
                        qrBitmap.set(
                                ((android.graphics.drawable.BitmapDrawable)
                                                ((ImageView)
                                                                current()
                                                                        .findViewById(
                                                                                R.id.taxi_qr_image))
                                                        .getDrawable())
                                        .getBitmap()));
        android.graphics.Bitmap bitmap = qrBitmap.get();
        int[] pixels = new int[bitmap.getWidth() * bitmap.getHeight()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        com.google.zxing.RGBLuminanceSource source =
                new com.google.zxing.RGBLuminanceSource(
                        bitmap.getWidth(), bitmap.getHeight(), pixels);
        assertEquals(
                qr,
                new com.google.zxing.MultiFormatReader()
                        .decode(
                                new com.google.zxing.BinaryBitmap(
                                        new com.google.zxing.common.HybridBinarizer(source)))
                        .getText());

        login(driverEmail, UserRole.TAXISTA, driverUid);
        open(FinalizarServicioActivity.class, 1001);
        waitFor(() -> current() instanceof FinalizarServicioActivity);
        ins.runOnMainSync(
                () -> ((EditText) current().findViewById(R.id.et_codigo_manual)).setText(qr));
        click(R.id.btn_validar_codigo);
        waitFor(() -> current() instanceof ServicioFinalizadoActivity);
        assertNull(TaxiApis.get(context).miServicioActivo());
        assertEquals(1, TaxiApis.get(context).miHistorial(50).optJSONArray("servicios").length());
        login(clientEmail, UserRole.CLIENT, clientUid);
        open(CalificarTaxiActivity.class, 1001);
        waitFor(
                () -> {
                    AtomicBoolean ready = new AtomicBoolean();
                    ins.runOnMainSync(
                            () ->
                                    ready.set(
                                            current()
                                                    .findViewById(R.id.btn_enviar_calificacion)
                                                    .isEnabled()));
                    return ready.get();
                });
        ins.runOnMainSync(
                () ->
                        ((EditText) current().findViewById(R.id.et_comentarios_taxi))
                                .setText("Viaje de prueba"));
        click(R.id.iv_taxi_star_5);
        screenshot("taxi-api-calificacion");
        click(R.id.btn_enviar_calificacion);
        waitFor(() -> current() instanceof MainActivity);
        assertEquals(
                5,
                TaxiApis.get(context)
                        .verServicio(1001)
                        .optJSONObject("calificacion")
                        .optInt("puntuacion"));
        assertNotNull(ReservaRepository.getInstance(context).buscarPorId(1001));        closeAll();
        auth.signOut();
        String saEmail="sa-"+System.nanoTime()+"@example.test";
        String saUid=Tasks.await(auth.createUserWithEmailAndPassword(saEmail,password),20,TimeUnit.SECONDS).getUser().getUid();
        seed("users/"+saUid,"uid",saUid,"name","Superadmin QA","email",saEmail,"role","SUPERADMIN","active",true);
        login(saEmail,UserRole.SUPERADMIN,saUid);
        long driverId=TaxiApis.get(context).listarTaxistas(null).optJSONArray("taxistas").getJSONObject(0).getLong("id");
        open(com.example.proyecto_iotelito.ui.superadmin.usuarios.TaxistasAdminActivity.class,1001);
        waitFor(()->text(R.id.tv_taxi_admin_status,"1 taxistas"));
        assertTrue(TaxiApis.get(context).cambiarActivoTaxista(driverId,false,"Prueba local de integración").has("activo"));
        click(R.id.btn_taxi_admin_refresh);
        waitFor(()->text(R.id.btn_taxi_admin_toggle,"Activar"));
        screenshot("superadmin-taxistas-integrado");
        TaxiApis.get(context).cambiarActivoTaxista(driverId,true,"Prueba local finalizada");
    }
}
