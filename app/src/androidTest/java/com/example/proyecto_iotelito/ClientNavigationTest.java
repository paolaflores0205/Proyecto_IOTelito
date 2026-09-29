package com.example.proyecto_iotelito;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.view.View;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

/** Verifica el tamaño real y que las cuatro opciones no estén recortadas. */
@RunWith(AndroidJUnit4.class)
public class ClientNavigationTest {
    @Test
    public void menuClienteQuedaEncimaDeLaBarraDelSistema() {
        android.app.Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Intent intent = new Intent(instrumentation.getTargetContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = instrumentation.startActivitySync(intent);
        try {
            instrumentation.waitForIdleSync();
            instrumentation.runOnMainSync(() -> {
                View menu = activity.findViewById(R.id.bottom_nav);
                View decor = activity.getWindow().getDecorView();
                WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
                assertNotNull(insets);
                int alturaEsperada = Math.round(64 * activity.getResources().getDisplayMetrics().density);
                assertEquals(alturaEsperada, menu.getHeight());
                assertEquals(0, menu.getPaddingBottom());
                int[] posicionMenu = new int[2];
                int[] posicionDecor = new int[2];
                menu.getLocationOnScreen(posicionMenu);
                decor.getLocationOnScreen(posicionDecor);
                int limiteSeguro = posicionDecor[1] + decor.getHeight()
                        - insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
                assertEquals(limiteSeguro, posicionMenu[1] + menu.getHeight());
                int[] opciones = {R.id.nav_explorar, R.id.nav_reservas, R.id.nav_mensajes, R.id.nav_perfil};
                for (int opcion : opciones) {
                    View item = menu.findViewById(opcion);
                    assertNotNull(item);
                    Rect visible = new Rect();
                    assertTrue(item.getGlobalVisibleRect(visible));
                    assertEquals("Opción recortada", item.getHeight(), visible.height());
                }
            });
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }
}
