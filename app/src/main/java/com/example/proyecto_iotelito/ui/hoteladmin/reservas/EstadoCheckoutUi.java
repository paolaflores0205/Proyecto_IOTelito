package com.example.proyecto_iotelito.ui.hoteladmin.reservas;

import android.content.Context;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;

/**
 * Mapea {@link ReservaAdmin.EstadoCheckout} a texto y colores (pill de estado),
 * compartido entre el listado de reservas y el detalle de cobro/checkout.
 * Análogo a {@link com.example.proyecto_iotelito.ui.reservas.EstadoUi} del cliente.
 */
public final class EstadoCheckoutUi {

    private EstadoCheckoutUi() {
    }

    public static String texto(Context context, ReservaAdmin.EstadoCheckout estado) {
        switch (estado) {
            case PROXIMA:
                return context.getString(R.string.hoteladmin_estado_proxima);
            case HOSPEDADO:
                return context.getString(R.string.hoteladmin_estado_hospedado);
            case CHECKOUT_PENDIENTE:
                return context.getString(R.string.hoteladmin_estado_checkout);
            default:
                return context.getString(R.string.hoteladmin_estado_finalizada);
        }
    }

    public static int colorFondo(ReservaAdmin.EstadoCheckout estado) {
        switch (estado) {
            case PROXIMA:
                return R.color.io_info_bg;
            case HOSPEDADO:
                return R.color.io_success_bg;
            case CHECKOUT_PENDIENTE:
                return R.color.io_warning_bg;
            default:
                return R.color.io_tag_bg;
        }
    }

    public static int colorTexto(ReservaAdmin.EstadoCheckout estado) {
        switch (estado) {
            case PROXIMA:
                return R.color.io_teal;
            case HOSPEDADO:
                return R.color.io_success_text;
            case CHECKOUT_PENDIENTE:
                return R.color.io_warning_text;
            default:
                return R.color.io_text_secondary;
        }
    }
}
