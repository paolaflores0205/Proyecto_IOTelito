package com.example.proyecto_iotelito.ui.taxista;

import android.content.Context;
import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;

/**
 * Mapea {@link EstadoServicio} a texto y colores del pill de estado,
 * siguiendo el mismo patrón que EstadoUi en el flujo de reservas del cliente.
 */
final class EstadoServicioUi {

    private EstadoServicioUi() {
    }

    static String texto(Context context, EstadoServicio estado) {
        switch (estado) {
            case SOLICITADO:
                return context.getString(R.string.taxi_estado_solicitado);
            case ASIGNADO:
                return context.getString(R.string.taxi_estado_asignado);
            case EN_CAMINO:
                return context.getString(R.string.taxi_estado_en_camino);
            case EN_TRASLADO:
                return context.getString(R.string.taxi_estado_en_traslado);
            case FINALIZADO:
                return context.getString(R.string.taxi_estado_finalizado);
            default:
                return context.getString(R.string.taxi_estado_cancelado);
        }
    }

    static int colorFondo(EstadoServicio estado) {
        return estado == EstadoServicio.CANCELADO ? R.color.io_danger_bg : R.color.io_teal_light;
    }

    static int colorTexto(EstadoServicio estado) {
        return estado == EstadoServicio.CANCELADO ? R.color.io_danger_text : R.color.io_teal;
    }

    /** Pinta un pill de estado (texto + colores de fondo/texto) en un solo paso. */
    static void pintarPill(TextView pill, EstadoServicio estado) {
        Context context = pill.getContext();
        pill.setText(texto(context, estado));
        pill.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(context, colorFondo(estado))));
        pill.setTextColor(ContextCompat.getColor(context, colorTexto(estado)));
    }
}
