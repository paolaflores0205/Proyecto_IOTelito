package com.example.proyecto_iotelito;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.SystemClock;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.data.superadmin.FirebaseUsersRepository;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.example.proyecto_iotelito.ui.auth.LoginActivity;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminMainActivity;
import com.google.firebase.auth.FirebaseAuth;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class SuperadminAccessTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();
    @Test public void localSuperadminCannotBecomeAuthenticatedSession() {
        assertThrows(IllegalArgumentException.class, () -> SessionRepository.guardar(ins.getTargetContext(),
                new AuthenticatedUser("superadmin@iotelito.pe", "Demo", UserRole.SUPERADMIN)));
    }
    @Test public void openingSuperadminWithoutFirebaseRedirectsToLogin() {
        assumeTrue("No cerrar una cuenta real para ejecutar esta prueba", FirebaseAuth.getInstance().getCurrentUser() == null);
        ins.runOnMainSync(() -> ins.getTargetContext().startActivity(new Intent(ins.getTargetContext(),
                SuperadminMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
        AtomicReference<Activity> login = new AtomicReference<>();
        long limit = SystemClock.uptimeMillis() + 15000;
        while (login.get() == null && SystemClock.uptimeMillis() < limit) {
            ins.runOnMainSync(() -> {
                for (Activity activity : ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED))
                    if (activity instanceof LoginActivity) login.set(activity);
            });
            if (login.get() == null) SystemClock.sleep(100);
        }
        assertNotNull("El panel debe rechazar el acceso sin sesión Firebase", login.get());
        ins.runOnMainSync(() -> login.get().finish());
    }
    @Test public void usersListWithoutFirebaseDoesNotExposeDemoUsers() {
        assumeTrue(FirebaseAuth.getInstance().getCurrentUser() == null);
        LiveData<FirebaseUsersRepository.State> data = new FirebaseUsersRepository().observeUsers();
        AtomicReference<FirebaseUsersRepository.State> state = new AtomicReference<>();
        Observer<FirebaseUsersRepository.State> observer = state::set;
        try {
            ins.runOnMainSync(() -> data.observeForever(observer));
            long limit = SystemClock.uptimeMillis() + 5000;
            while ((state.get() == null || state.get().loading) && SystemClock.uptimeMillis() < limit) SystemClock.sleep(50);
            assertNotNull(state.get()); assertFalse(state.get().loading);
            assertTrue(state.get().users.isEmpty()); assertNotNull(state.get().error);
        } finally { ins.runOnMainSync(() -> data.removeObserver(observer)); }
    }
}
