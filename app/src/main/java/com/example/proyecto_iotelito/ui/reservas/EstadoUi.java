package com.example.proyecto_iotelito.ui.reservas;

import android.content.Context;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.Reserva;

/**
 * Mapea {@link Reserva.Estado} a texto y colores (pill de estado),
 * compartido entre el listado de reservas y su detalle.
 */
final class EstadoUi {

    private EstadoUi() {
    }

    static String texto(Context context, Reserva.Estado estado) {
        switch (estado) {
            case CONFIRMADA:
                return context.getString(R.string.estado_confirmada);
            case PENDIENTE_PAGO:
                return context.getString(R.string.estado_pendiente_pago);
            case COMPLETADA:
                return context.getString(R.string.estado_completada);
            default:
                return context.getString(R.string.estado_cancelada);
        }
    }

    static int colorFondo(Reserva.Estado estado) {
        switch (estado) {
            case CONFIRMADA:
                return R.color.io_teal_light;
            case PENDIENTE_PAGO:
                return R.color.io_warning_bg;
            case CANCELADA:
                return R.color.io_danger_bg;
            default:
                return R.color.io_tag_bg;
        }
    }

    static int colorTexto(Reserva.Estado estado) {
        switch (estado) {
            case CONFIRMADA:
                return R.color.io_teal;
            case PENDIENTE_PAGO:
                return R.color.io_warning_text;
            case CANCELADA:
                return R.color.io_danger_text;
            default:
                return R.color.io_text_secondary;
        }
    }

    static int mensaje(Reserva.Estado estado) {
        switch (estado) {
            case CONFIRMADA:
                return R.string.mensaje_estado_confirmada;
            case PENDIENTE_PAGO:
                return R.string.mensaje_estado_pendiente;
            case COMPLETADA:
                return R.string.mensaje_estado_completada;
            default:
                return R.string.mensaje_estado_cancelada;
        }
    }
}
