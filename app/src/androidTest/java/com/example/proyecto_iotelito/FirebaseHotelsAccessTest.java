package com.example.proyecto_iotelito;

import android.app.Instrumentation;
import android.os.SystemClock;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.data.superadmin.FirebaseHotelsRepository;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class FirebaseHotelsAccessTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    private FirebaseHotelsRepository repository() {
        assumeTrue("No cerrar una cuenta real durante las pruebas", FirebaseAuth.getInstance().getCurrentUser() == null);
        return new FirebaseHotelsRepository(ins.getTargetContext(), true);
    }
    @Test public void noSessionDoesNotExposeLocalCatalogAsRemoteHotels() {
        LiveData<FirebaseHotelsRepository.State> data = repository().observe();
        AtomicReference<FirebaseHotelsRepository.State> state = new AtomicReference<>();
        Observer<FirebaseHotelsRepository.State> observer = state::set;
        try {
            ins.runOnMainSync(() -> data.observeForever(observer));
            long limit = SystemClock.uptimeMillis() + 5000;
            while ((state.get() == null || state.get().loading) && SystemClock.uptimeMillis() < limit) SystemClock.sleep(50);
            assertNotNull(state.get()); assertFalse(state.get().loading);
            assertNotNull(state.get().error); assertTrue(state.get().hotels.isEmpty());
        } finally { ins.runOnMainSync(() -> data.removeObserver(observer)); }
    }
    @Test public void saveWithoutSessionDoesNotFallBackToRoom() {
        FirebaseHotelsRepository repo = repository();
        ManagedHotel hotel = new ManagedHotel(0, "Prueba", "Av. 1", "Miraflores", "Sin asignar", 0, 0, 1, true);
        hotel.city = "Lima"; hotel.latitude = -12.0; hotel.longitude = -77.0;
        AtomicReference<String> error = new AtomicReference<>();
        repo.save(hotel, (id, message) -> error.set(message));
        assertNotNull(error.get());
    }
    @Test public void assignWithoutSessionDoesNotUseLocalUserIds() {
        AtomicReference<String> error = new AtomicReference<>();
        repository().assign(1, "uid-de-prueba", (id, message) -> error.set(message));
        assertNotNull(error.get());
    }
}
