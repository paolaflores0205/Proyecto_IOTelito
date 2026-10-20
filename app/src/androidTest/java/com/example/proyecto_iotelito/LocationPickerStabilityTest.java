package com.example.proyecto_iotelito;

import android.app.*;
import android.content.*;
import android.os.Handler;
import android.webkit.*;
import android.widget.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.ui.superadmin.hoteles.SeleccionarUbicacionActivity;
import java.lang.reflect.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LocationPickerStabilityTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();

    private void set(Activity activity, String name, Object value) throws Exception {
        Field field = SeleccionarUbicacionActivity.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(activity, value);
    }

    private Object get(Activity activity, String name) throws Exception {
        Field field = SeleccionarUbicacionActivity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(activity);
    }

    private Activity open() {
        Activity activity = ins.startActivitySync(new Intent(ins.getTargetContext(),
                SeleccionarUbicacionActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        ins.runOnMainSync(() -> {
            try {
                // Estas pruebas no consultan direcciones reales ni necesitan conectividad.
                WebView map = activity.findViewById(R.id.map_web_view);
                map.stopLoading();
                map.setWebViewClient(new WebViewClient());
                set(activity, "pageLoaded", true);
                ((Handler) get(activity, "handler")).removeCallbacksAndMessages(null);
                set(activity, "generation", 500);
                map.loadData("<html><body>Mapa de prueba</body></html>", "text/html", "UTF-8");
            } catch (Exception error) { throw new AssertionError(error); }
        });
        ins.waitForIdleSync();
        return activity;
    }

    private void showText(Activity activity, String text) throws Exception {
        CountDownLatch layout = new CountDownLatch(1);
        ins.runOnMainSync(() -> {
            TextView label = activity.findViewById(R.id.tv_selected_coordinates);
            label.getViewTreeObserver().addOnGlobalLayoutListener(
                    new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override public void onGlobalLayout() {
                            label.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            layout.countDown();
                        }
                    });
            label.setText(text);
            label.requestLayout();
        });
        assertTrue("El mapa no terminó de medirse", layout.await(12, TimeUnit.SECONDS));
        ins.waitForIdleSync();
    }

    @Test public void repeatedCenterKeepsAddressAndConfirmEnabled() {
        Activity activity = open();
        try {
            ins.runOnMainSync(() -> {
                try {
                    set(activity, "hasRequestedPoint", true);
                    set(activity, "requestedLatitude", -12.0464);
                    set(activity, "requestedLongitude", -77.0428);
                    set(activity, "address", "Hotel seleccionado");
                    Button confirm = activity.findViewById(R.id.btn_confirm_location);
                    confirm.setEnabled(true);
                    Method resolve = SeleccionarUbicacionActivity.class.getDeclaredMethod(
                            "resolve", double.class, double.class, boolean.class);
                    resolve.setAccessible(true);
                    int generation = (int) get(activity, "generation");
                    for (int i = 0; i < 20; i++) {
                        resolve.invoke(activity, -12.0463999, -77.0428, false);
                    }
                    assertEquals(generation, get(activity, "generation"));
                    assertEquals("Hotel seleccionado", get(activity, "address"));
                    assertTrue(confirm.isEnabled());
                    // Un punto realmente diferente sigue invalidando la dirección anterior.
                    resolve.invoke(activity, -12.047, -77.043, false);
                    assertEquals(generation + 1, get(activity, "generation"));
                    assertFalse(confirm.isEnabled());
                    // Buscar explícitamente la misma dirección permite reintentarla.
                    set(activity, "address", "Otra dirección");
                    confirm.setEnabled(true);
                    resolve.invoke(activity, -12.047, -77.043, true);
                    assertEquals(generation + 2, get(activity, "generation"));
                    assertFalse(confirm.isEnabled());
                    ((Handler) get(activity, "handler")).removeCallbacksAndMessages(null);
                } catch (Exception error) { throw new AssertionError(error); }
            });
        } finally { ins.runOnMainSync(activity::finish); }
    }

    @Test public void addressLengthDoesNotResizeMap() throws Exception {
        Activity activity = open();
        int[] heights = new int[2];
        try {
            showText(activity, "Obteniendo dirección del punto…");
            ins.runOnMainSync(() -> heights[0] = activity.findViewById(R.id.map_web_view).getHeight());
            showText(activity, "Avenida de prueba, Hotel seleccionado\nDistrito: Miraflores\nCiudad: Lima");
            ins.runOnMainSync(() -> heights[1] = activity.findViewById(R.id.map_web_view).getHeight());
            assertTrue(heights[0] > 0);
            assertEquals(heights[0], heights[1]);
        } finally { ins.runOnMainSync(activity::finish); }
    }
}
