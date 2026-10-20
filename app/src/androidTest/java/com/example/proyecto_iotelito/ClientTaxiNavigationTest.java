package com.example.proyecto_iotelito;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.data.local.IoTelitoDatabase;
import com.example.proyecto_iotelito.data.local.cliente.TaxiDatabase;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.example.proyecto_iotelito.ui.pago.CheckoutActivity;
import com.example.proyecto_iotelito.ui.reservas.DetalleReservaActivity;
import com.example.proyecto_iotelito.ui.taxi.TaxistaAsignadoActivity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ClientTaxiNavigationTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();
    private AuthenticatedUser anterior;
    private Reserva reserva;
    private int solicitudId = -1;

    @Before
    public void preparar() throws Exception {
        anterior = SessionRepository.obtener(context);
        SessionRepository.guardar(context, new AuthenticatedUser(UUID.randomUUID() + "@cliente.test", "Cliente de prueba", UserRole.CLIENT));
        try (android.os.ParcelFileDescriptor ignored = instrumentation.getUiAutomation().executeShellCommand(
                "pm grant " + context.getPackageName() + " " + Manifest.permission.POST_NOTIFICATIONS)) { }
        ReservaRepository repository = ReservaRepository.getInstance(context);
        barrera();
        Hotel hotel = SampleData.findById(4);
        reserva = repository.agregarReserva(hotel, LocalDate.now().minusDays(4), LocalDate.now(),
                "2 adultos", 1280, "TEST-TAXI-" + UUID.randomUUID(), hotel.roomName, "310");
        solicitudId = TaxiRepository.crear(context, reserva, hotel.name, hotel.address, "15:30", 2, 1);
        assertTrue(solicitudId >= 0);
        repository.actualizarEstado(reserva.id, Reserva.Estado.COMPLETADA);
        barrera();
    }

    private void barrera() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ClientLocalTasks.execute(latch::countDown);
        assertTrue(latch.await(15, TimeUnit.SECONDS));
    }

    @After
    public void limpiar() throws Exception {
        instrumentation.runOnMainSync(() -> {
            for (Stage stage : new Stage[]{Stage.RESUMED, Stage.STARTED, Stage.CREATED, Stage.STOPPED}) {
                for (Activity activity : new ArrayList<>(ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(stage))) {
                    if (activity instanceof DetalleReservaActivity || activity instanceof CheckoutActivity
                            || activity instanceof TaxistaAsignadoActivity) activity.finish();
                }
            }
        });
        barrera();
        if (reserva != null) {
            IoTelitoDatabase.getInstance(context).getOpenHelper().getWritableDatabase()
                    .delete("reservas_cliente", "id = ?", new Object[]{reserva.id});
        }
        if (solicitudId >= 0) {
            TaxiDatabase.getInstance(context).getOpenHelper().getWritableDatabase()
                    .delete("servicios_taxi", "id = ?", new Object[]{solicitudId});
            int notificationId = 1_000_000 + solicitudId * 4;
            IoTelitoDatabase.getInstance(context).getOpenHelper().getWritableDatabase()
                    .delete("notificaciones_cliente", "id = ?", new Object[]{notificationId});
            context.getSystemService(NotificationManager.class).cancel(notificationId);
        }
        if (anterior == null) SessionRepository.cerrar(context);
        else SessionRepository.guardar(context, anterior);
    }

    private void comprobarRedireccion(Class<? extends Activity> pantalla) {
        instrumentation.startActivitySync(new Intent(context, pantalla)
                .putExtra(DetalleReservaActivity.EXTRA_RESERVA_ID, reserva.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        AtomicReference<Activity> actual = new AtomicReference<>();
        long limite = SystemClock.uptimeMillis() + 15000;
        while (actual.get() == null && SystemClock.uptimeMillis() < limite) {
            instrumentation.runOnMainSync(() -> {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)) {
                    if (activity instanceof TaxistaAsignadoActivity) actual.set(activity);
                }
            });
            SystemClock.sleep(30);
        }
        assertNotNull("Debe abrir el taxi existente", actual.get());
        assertEquals(solicitudId, actual.get().getIntent().getIntExtra(TaxiRepository.EXTRA_ID, -1));
        assertFalse(ReservaRepository.getInstance(context).buscarPorId(reserva.id).puedeHacerCheckout());
    }

    @Test public void detalleDeReservaAbreSuTaxiExistente() { comprobarRedireccion(DetalleReservaActivity.class); }
    @Test public void checkoutFinalizadoAbreElTaxiSinRepetirElCobro() { comprobarRedireccion(CheckoutActivity.class); }
}
