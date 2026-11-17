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
    /** También admite NestedScrollView y campos multilínea sin cambiar su configuración. */
    public static void keepFocusedFieldVisible(Activity activity, View root) {
        Runnable reveal = () -> {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(root);
            View focused = activity.getCurrentFocus();
            if (insets == null || !insets.isVisible(WindowInsetsCompat.Type.ime())
                    || !(focused instanceof EditText) || !focused.isShown()) return;
            int margin = Math.round(24 * root.getResources().getDisplayMetrics().density);
            int viewportHeight = root.getHeight() - root.getPaddingTop() - root.getPaddingBottom();
            View target = focused;
            ViewParent parent = focused.getParent();
            while (parent instanceof View && parent != root) {
                if (parent instanceof TextInputLayout) {
                    if (((View) parent).getHeight() + 2 * margin < viewportHeight) target = (View) parent;
                    break;
                }
                parent = parent.getParent();
            }
            Rect rectangle = new Rect();
            if (target == focused) focused.getFocusedRect(rectangle);
            else rectangle.set(0, 0, target.getWidth(), target.getHeight());
            rectangle.top -= margin;
            rectangle.bottom += margin;
            target.requestRectangleOnScreen(rectangle, false);
        };
        // El contenedor mantiene su tamaño y cambia su padding al recibir el IME;
        // observar el árbol también detecta la reducción de sus hijos desplazables.
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> root.post(reveal));
        root.getViewTreeObserver().addOnGlobalFocusChangeListener((oldFocus, newFocus) -> root.post(reveal));
    }
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
