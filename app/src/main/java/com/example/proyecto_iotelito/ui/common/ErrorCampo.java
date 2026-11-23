package com.example.proyecto_iotelito.ui.common;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Muestra el error de un campo como texto rojo debajo de él (en lugar del globo con "!").
 * Si el campo está dentro de un TextInputLayout usa su error; si no, agrega un TextView
 * justo debajo. El mensaje se borra solo cuando el usuario vuelve a escribir.
 */
public final class ErrorCampo {

    private static final int TAG_ERROR = R.id.tag_error_campo;
    private static final int TAG_VIGILADO = R.id.tag_error_vigilado;

    private ErrorCampo() { }

    public static void mostrar(EditText campo, String mensaje) {
        TextInputLayout layout = layoutDe(campo);
        if (layout != null) {
            layout.setError(mensaje);
        } else {
            TextView error = textoDe(campo, true);
            if (error == null) {
                campo.setError(mensaje); // Contenedor sin orden vertical: se usa el aviso estándar.
            } else {
                error.setText(mensaje);
                error.setVisibility(View.VISIBLE);
            }
        }
        vigilar(campo);
    }

    public static void limpiar(EditText campo) {
        TextInputLayout layout = layoutDe(campo);
        if (layout != null) layout.setError(null);
        TextView error = textoDe(campo, false);
        if (error != null) error.setVisibility(View.GONE);
        campo.setError(null);
    }

    /** Muestra o limpia según haya mensaje. Devuelve true si el campo es válido. */
    public static boolean aplicar(EditText campo, String mensaje) {
        if (mensaje == null || mensaje.isEmpty()) {
            limpiar(campo);
            return true;
        }
        mostrar(campo, mensaje);
        return false;
    }

    private static TextInputLayout layoutDe(EditText campo) {
        ViewParent padre = campo.getParent();
        for (int i = 0; i < 2 && padre != null; i++, padre = padre.getParent()) {
            if (padre instanceof TextInputLayout) return (TextInputLayout) padre;
        }
        return null;
    }

    private static TextView textoDe(EditText campo, boolean crear) {
        Object guardado = campo.getTag(TAG_ERROR);
        if (guardado instanceof TextView) return (TextView) guardado;
        if (!crear || !(campo.getParent() instanceof LinearLayout)) return null;
        LinearLayout padre = (LinearLayout) campo.getParent();
        if (padre.getOrientation() != LinearLayout.VERTICAL) return null;
        TextView error = new TextView(campo.getContext());
        error.setTextColor(ContextCompat.getColor(campo.getContext(), R.color.io_danger_text));
        error.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        int arriba = Math.round(4 * campo.getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = arriba;
        padre.addView(error, padre.indexOfChild(campo) + 1, params);
        campo.setTag(TAG_ERROR, error);
        return error;
    }

    private static void vigilar(EditText campo) {
        if (campo.getTag(TAG_VIGILADO) != null) return;
        campo.setTag(TAG_VIGILADO, Boolean.TRUE);
        campo.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { limpiar(campo); }
        });
    }
}
