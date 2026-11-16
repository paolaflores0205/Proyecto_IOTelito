package com.example.proyecto_iotelito.ui.common;

import android.app.Activity;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewParent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ScrollView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.textfield.TextInputLayout;

/** El contenedor de la aplicación reserva el IME; aquí se mantiene visible el campo y su etiqueta. */
public final class FormKeyboard {
    private FormKeyboard() { }
    public static void attach(Activity activity, ScrollView scroll, EditText... fields) {
        Runnable reveal = () -> {
            View focused = activity.getCurrentFocus();
            if (!(focused instanceof EditText)) return;
            View target = focused;
            ViewParent parent = focused.getParent();
            while (parent instanceof View && parent != scroll) {
                if (parent instanceof TextInputLayout) { target = (View) parent; break; }
                parent = parent.getParent();
            }
            int margin = Math.round(24 * scroll.getResources().getDisplayMetrics().density);
            target.requestRectangleOnScreen(new Rect(0, -margin, target.getWidth(), target.getHeight() + margin), false);
        };
        ViewCompat.setOnApplyWindowInsetsListener(scroll, (view, insets) -> {
            if (insets.isVisible(WindowInsetsCompat.Type.ime())) scroll.post(reveal);
            return insets;
        });
        scroll.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            if (b-t != ob-ot) scroll.post(reveal);
        });
        for (int i=0; i<fields.length; i++) {
            EditText field = fields[i];
            final int next = i+1;
            field.setImeOptions(next < fields.length ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
            field.setSingleLine(true);
            if (next < fields.length) field.setNextFocusForwardId(fields[next].getId());
            field.setOnFocusChangeListener((view, hasFocus) -> { if (hasFocus) scroll.post(reveal); });
            field.setOnEditorActionListener((view, action, event) -> {
                if (action == EditorInfo.IME_ACTION_NEXT && next < fields.length) {
                    fields[next].requestFocus(); return true;
                }
                if (action == EditorInfo.IME_ACTION_DONE) {
                    WindowCompat.getInsetsController(activity.getWindow(), scroll).hide(WindowInsetsCompat.Type.ime());
                    field.clearFocus(); return true;
                }
                return false;
            });
        }
        ViewCompat.requestApplyInsets(scroll);
    }
}
