package com.example.proyecto_iotelito;

import android.Manifest;
import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.SystemClock;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminMainActivity;
import com.example.proyecto_iotelito.ui.superadmin.hoteles.FormularioHotelActivity;
import com.example.proyecto_iotelito.ui.superadmin.hoteles.AsignarAdministradorActivity;
import com.example.proyecto_iotelito.ui.superadmin.notificaciones.AvisosSuperadminActivity;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SuperadminUiTest {
    private final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();

    private Activity open(Class<? extends Activity> activity, String key, int id) {
        instrumentation.getUiAutomation().grantRuntimePermission(
                instrumentation.getTargetContext().getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        Intent intent = new Intent(instrumentation.getTargetContext(), activity);
        if (key != null) intent.putExtra(key, id);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return instrumentation.startActivitySync(intent);
    }

    private void await(BooleanSupplier condition) {
        long limit = SystemClock.uptimeMillis() + 10000;
        AtomicBoolean done = new AtomicBoolean(false);
        while (!done.get() && SystemClock.uptimeMillis() < limit) {
            instrumentation.runOnMainSync(() -> done.set(condition.getAsBoolean()));
            if (!done.get()) SystemClock.sleep(50);
        }
        assertTrue("La pantalla no recibió los datos locales", done.get());
        instrumentation.waitForIdleSync();
    }

    @Test public void menuBlancoDe64dpMantieneOpcionesVisiblesYNavegacion() {
        Activity activity = open(SuperadminMainActivity.class, null, 0);
        try {
            await(() -> !"0".contentEquals(((TextView) activity.findViewById(R.id.tv_dashboard_hotels)).getText()));
            instrumentation.runOnMainSync(() -> {
                View menu = activity.findViewById(R.id.superadmin_bottom_nav);
                int expectedHeight = Math.round(64 * activity.getResources().getDisplayMetrics().density);
                assertEquals(expectedHeight, menu.getHeight());
                assertEquals(Color.WHITE, ((ColorDrawable) menu.getBackground()).getColor());
                int[] position = new int[2];
                menu.getLocationOnScreen(position);
                View decor = activity.getWindow().getDecorView();
                WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
                assertNotNull(insets);
                int[] decorPosition = new int[2];
                decor.getLocationOnScreen(decorPosition);
                assertEquals(decorPosition[1] + decor.getHeight() - insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom,
                        position[1] + menu.getHeight());
                int[] buttons = {R.id.nav_superadmin_home_button, R.id.nav_superadmin_users_button,
                        R.id.nav_superadmin_hotels_button, R.id.nav_superadmin_reports_button};
                for (int id : buttons) {
                    View button = activity.findViewById(id);
                    Rect visible = new Rect();
                    assertTrue(button.getGlobalVisibleRect(visible));
                    assertEquals(button.getHeight(), visible.height());
                }
                activity.findViewById(R.id.nav_superadmin_users_button).performClick();
            });
            await(() -> activity.findViewById(R.id.rv_users) != null
                    && ((RecyclerView) activity.findViewById(R.id.rv_users)).getAdapter().getItemCount() >= 16);
            instrumentation.runOnMainSync(() -> activity.findViewById(R.id.nav_superadmin_hotels_button).performClick());
            await(() -> activity.findViewById(R.id.rv_hotels) != null
                    && ((RecyclerView) activity.findViewById(R.id.rv_hotels)).getAdapter().getItemCount() >= 12);
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }

    @Test public void formularioValidaYAsignacionCargaAdministradoresReales() {
        Activity form = open(FormularioHotelActivity.class, FormularioHotelActivity.EXTRA_HOTEL_ID, 1);
        try {
            await(() -> form.findViewById(R.id.btn_save).isEnabled());
            instrumentation.runOnMainSync(() -> {
                assertFalse(((TextView) form.findViewById(R.id.et_name)).getText().toString().isEmpty());
                ((TextInputEditText) form.findViewById(R.id.et_name)).setText("");
                form.findViewById(R.id.btn_save).performClick();
                assertNotNull(((TextInputLayout) form.findViewById(R.id.til_name)).getError());
                assertFalse(form.isFinishing());
            });
        } finally { instrumentation.runOnMainSync(form::finish); }
        Activity assign = open(AsignarAdministradorActivity.class, AsignarAdministradorActivity.EXTRA_HOTEL_ID, 1);
        try {
            await(() -> ((RadioGroup) assign.findViewById(R.id.radio_group_admins)).getChildCount() > 0);
            instrumentation.runOnMainSync(() -> {
                RadioGroup group = assign.findViewById(R.id.radio_group_admins);
                assertEquals(4, group.getChildCount());
                assertTrue(((TextView) group.getChildAt(0)).getText().toString().contains("Carlos Mendoza"));
                group.check(group.getChildAt(0).getId());
                assertTrue(assign.findViewById(R.id.btn_assign).isEnabled());
            });
        } finally { instrumentation.runOnMainSync(assign::finish); }
        Activity alerts = open(AvisosSuperadminActivity.class, null, 0);
        try {
            await(() -> ((RecyclerView) alerts.findViewById(R.id.rv_alerts)).getAdapter().getItemCount() >= 3);
        } finally { instrumentation.runOnMainSync(alerts::finish); }
    }
}
