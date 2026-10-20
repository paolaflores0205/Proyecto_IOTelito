package com.example.proyecto_iotelito;

import android.Manifest;
import android.app.*;
import android.content.Intent;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.*;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.ui.hoteladmin.HotelAdminMainActivity;
import com.example.proyecto_iotelito.ui.hoteladmin.mensajes.AdminChatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HotelAdminInsetsTest {
    private final Instrumentation ins = InstrumentationRegistry.getInstrumentation();

    private Activity open(Class<? extends Activity> type) {
        ins.getUiAutomation().grantRuntimePermission(ins.getTargetContext().getPackageName(),
                Manifest.permission.POST_NOTIFICATIONS);
        return ins.startActivitySync(new Intent(ins.getTargetContext(), type)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    private void await(BooleanSupplier condition) {
        long limit = SystemClock.uptimeMillis() + 15000;
        AtomicBoolean ready = new AtomicBoolean();
        while (!ready.get() && SystemClock.uptimeMillis() < limit) {
            ins.runOnMainSync(() -> ready.set(condition.getAsBoolean()));
            if (!ready.get()) SystemClock.sleep(50);
        }
        assertTrue("La pantalla no terminó de ajustarse", ready.get());
        ins.waitForIdleSync();
    }

    private void assertLabelsVisible(View view) {
        if (view instanceof TextView && view.isShown()
                && ((TextView) view).getText().length() > 0) {
            Rect visible = new Rect();
            assertTrue(view.getGlobalVisibleRect(visible));
            assertEquals("Una etiqueta del menú está recortada", view.getHeight(), visible.height());
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) assertLabelsVisible(group.getChildAt(i));
        }
    }

    @Test public void adminMenuUsesOneBottomInsetAnd64dp() {
        Activity activity = open(HotelAdminMainActivity.class);
        try {
            await(() -> activity.findViewById(R.id.hoteladmin_bottom_nav).getHeight() > 0);
            ins.runOnMainSync(() -> {
                BottomNavigationView menu = activity.findViewById(R.id.hoteladmin_bottom_nav);
                View decor = activity.getWindow().getDecorView();
                WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
                assertNotNull(insets);
                int[] menuPosition = new int[2], decorPosition = new int[2];
                menu.getLocationOnScreen(menuPosition);
                decor.getLocationOnScreen(decorPosition);
                assertEquals(Math.round(64 * activity.getResources().getDisplayMetrics().density),
                        menu.getHeight());
                assertEquals(0, menu.getPaddingBottom());
                assertEquals(decorPosition[1] + decor.getHeight()
                                - insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom,
                        menuPosition[1] + menu.getHeight());
                assertLabelsVisible(menu);
            });
        } finally { ins.runOnMainSync(activity::finish); }
    }

    @Test public void threeButtonInsetIsNotAddedTwice() {
        Activity activity = open(HotelAdminMainActivity.class);
        try {
            await(() -> activity.findViewById(R.id.hoteladmin_bottom_nav).getHeight() > 0);
            ins.runOnMainSync(() -> {
                View content = activity.findViewById(android.R.id.content);
                View menu = activity.findViewById(R.id.hoteladmin_bottom_nav);
                int bottom = Math.round(48 * activity.getResources().getDisplayMetrics().density);
                WindowInsetsCompat simulated = new WindowInsetsCompat.Builder()
                        .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 24, 0, 0))
                        .setInsets(WindowInsetsCompat.Type.navigationBars(), Insets.of(0, 0, 0, bottom))
                        .build();
                for (int i = 0; i < 4; i++) {
                    ViewCompat.dispatchApplyWindowInsets(content, simulated);
                    assertEquals(0, content.getPaddingBottom());
                    assertEquals(0, menu.getPaddingBottom());
                    assertEquals(bottom, ((ViewGroup.MarginLayoutParams) menu.getLayoutParams()).bottomMargin);
                }
                ViewCompat.requestApplyInsets(content);
            });
        } finally { ins.runOnMainSync(activity::finish); }
    }

    @Test public void messageComposerRemainsAboveKeyboardAndKeepsDraft() {
        Activity activity = open(AdminChatActivity.class);
        String draft = "Mensaje de prueba\nSegunda línea\nTercera línea";
        try {
            await(() -> activity.findViewById(R.id.hoteladmin_chat_composer).getHeight() > 0
                    && activity.getWindow().getDecorView().hasWindowFocus());
            ins.runOnMainSync(() -> {
                EditText editor = activity.findViewById(R.id.et_mensaje);
                editor.setText(draft);
                editor.requestFocus();
                WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
                        .show(WindowInsetsCompat.Type.ime());
            });
            await(() -> {
                View decor = activity.getWindow().getDecorView();
                WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(decor);
                if (insets == null || !insets.isVisible(WindowInsetsCompat.Type.ime())) return false;
                int[] composer = new int[2], window = new int[2];
                View bar = activity.findViewById(R.id.hoteladmin_chat_composer);
                bar.getLocationOnScreen(composer);
                decor.getLocationOnScreen(window);
                int keyboardTop = window[1] + decor.getHeight()
                        - insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                return Math.abs(composer[1] + bar.getHeight() - keyboardTop) <= 2;
            });
            ins.runOnMainSync(() -> {
                View editor = activity.findViewById(R.id.et_mensaje);
                View send = activity.findViewById(R.id.btn_enviar);
                Rect visible = new Rect();
                assertTrue(editor.getGlobalVisibleRect(visible));
                assertEquals(editor.getHeight(), visible.height());
                assertTrue(send.getGlobalVisibleRect(visible));
                assertEquals(send.getHeight(), visible.height());
                assertEquals(draft, ((EditText) editor).getText().toString());
                WindowCompat.getInsetsController(activity.getWindow(), activity.getWindow().getDecorView())
                        .hide(WindowInsetsCompat.Type.ime());
            });
            await(() -> {
                WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(activity.getWindow().getDecorView());
                return insets != null && !insets.isVisible(WindowInsetsCompat.Type.ime());
            });
            ins.runOnMainSync(() -> assertEquals(draft,
                    ((EditText) activity.findViewById(R.id.et_mensaje)).getText().toString()));
        } finally { ins.runOnMainSync(activity::finish); }
    }
}
