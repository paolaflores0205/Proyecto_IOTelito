package com.example.proyecto_iotelito;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.TextView;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.data.local.IoTelitoDatabase;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.data.superadmin.local.*;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.example.proyecto_iotelito.ui.booking.DetalleHotelActivity;
import com.example.proyecto_iotelito.ui.pago.CheckoutActivity;
import com.example.proyecto_iotelito.ui.perfil.NotificacionesActivity;
import com.example.proyecto_iotelito.ui.reservas.DetalleReservaActivity;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ClientSuperadminIntegrationTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
    private final Context context = instrumentation.getTargetContext();

    private int save(ManagedHotel hotel) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<String> error = new AtomicReference<>();
        int[] id = {0};
        SuperadminRepository.getInstance(context).saveHotel(hotel, (value, message) -> {
            id[0] = value; error.set(message); done.countDown();
        });
        assertTrue(done.await(15, TimeUnit.SECONDS));
        assertNull(error.get());
        return id[0];
    }

    private void queueBarrier() throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        ClientLocalTasks.execute(done::countDown);
        assertTrue(done.await(15, TimeUnit.SECONDS));
    }

    @Test public void hotelNuevoSeReflejaEnClienteSinInventarDisponibilidad() throws Exception {
        String name = "Integración " + UUID.randomUUID();
        ManagedHotel hotel = new ManagedHotel(0, name, "Av. Integración 123", "Miraflores", "Sin asignar",
                0, 0, R.drawable.superadmin_hotel_hero, true);
        hotel.city = "Lima";
        hotel.description = "Descripción compartida";
        hotel.phone = "987654321";
        hotel.email = "integracion@example.com";
        hotel.latitude = -12.123;
        hotel.longitude = -77.034;
        int id = save(hotel);
        Activity screen = null;
        try {
            Hotel client = HotelCatalogRepository.buscar(context, id);
            assertEquals(name, client.name);
            assertEquals(hotel.address, client.address);
            assertEquals(hotel.description, client.description);
            assertEquals(hotel.latitude, client.latitude);
            assertEquals(hotel.email, client.contactEmail);
            assertEquals(0, client.pricePerNight, 0.001);
            assertFalse(client.hasInventory);
            CountDownLatch emitted = new CountDownLatch(1);
            AtomicReference<LiveData<List<Hotel>>> source = new AtomicReference<>();
            Observer<List<Hotel>> observer = rows -> {
                for (Hotel row : rows) if (row.id == id) emitted.countDown();
            };
            instrumentation.runOnMainSync(() -> {
                source.set(HotelCatalogRepository.observar(context));
                source.get().observeForever(observer);
            });
            try { assertTrue(emitted.await(10, TimeUnit.SECONDS)); }
            finally { instrumentation.runOnMainSync(() -> source.get().removeObserver(observer)); }
            screen = instrumentation.startActivitySync(new Intent(context, DetalleHotelActivity.class)
                    .putExtra(DetalleHotelActivity.EXTRA_HOTEL_ID, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            Activity current = screen;
            long deadline = SystemClock.uptimeMillis() + 10000;
            AtomicReference<String> title = new AtomicReference<>("");
            while (!name.equals(title.get()) && SystemClock.uptimeMillis() < deadline) {
                instrumentation.runOnMainSync(() -> title.set(((TextView) current.findViewById(R.id.tv_nombre)).getText().toString()));
                SystemClock.sleep(30);
            }
            assertEquals(name, title.get());
            instrumentation.runOnMainSync(() -> assertFalse(current.findViewById(R.id.btn_ver_habitaciones).isEnabled()));
        } finally {
            if (screen != null) { Activity current = screen; instrumentation.runOnMainSync(current::finish); }
            // Solo se retiran los registros creados por este test, identificados por ID y UUID.
            SuperadminDatabase db = SuperadminDatabase.getInstance(context);
            db.runInTransaction(() -> {
                db.dao().deleteAlert("hotel_pending_" + id);
                db.getOpenHelper().getWritableDatabase().delete("admin_hotels", "id = ?", new Object[]{id});
                db.getOpenHelper().getWritableDatabase().delete("admin_events", "detail = ?", new Object[]{name});
            });
            com.example.proyecto_iotelito.notification.SuperadminNotifications.cancel(context, "hotel_pending_" + id);
        }
    }

    @Test public void consultaInexistenteNoAbreOtraReservaYLosDetallesCarganSinUiThreadQueries() throws Exception {
        ReservaRepository repository = ReservaRepository.getInstance(context);
        queueBarrier();
        assertNull(repository.buscarPorId(-98765));
        List<Reserva> all = repository.obtenerTodas();
        assertFalse(all.isEmpty());
        int id = all.get(0).id;
        Class<?>[] screens = {DetalleReservaActivity.class, CheckoutActivity.class, NotificacionesActivity.class};
        for (Class<?> type : screens) {
            Activity screen = instrumentation.startActivitySync(new Intent(context, type)
                    .putExtra(DetalleReservaActivity.EXTRA_RESERVA_ID, id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            try {
                queueBarrier();
                instrumentation.waitForIdleSync();
                assertFalse("La pantalla no debe cerrarse al consultar Room", screen.isFinishing());
            } finally { instrumentation.runOnMainSync(screen::finish); }
        }
        // Se prueba la misma base versión 2 y se conserva la migración del cliente.
        assertEquals(2, IoTelitoDatabase.getInstance(context).getOpenHelper().getReadableDatabase().getVersion());
    }
}
