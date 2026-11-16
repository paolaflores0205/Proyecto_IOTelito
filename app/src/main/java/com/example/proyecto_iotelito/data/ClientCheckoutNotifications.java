package com.example.proyecto_iotelito.data;

import android.content.Context;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.NotificacionCliente;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.notification.ClientCheckoutAlerts;
import java.util.Collections;
import java.util.Locale;

/** Usa la misma bandeja y deduplicación que los avisos remotos al abrir un comprobante. */
public final class ClientCheckoutNotifications {
    private ClientCheckoutNotifications() { }
    public static void notificarUnaVez(Context context, Reserva reserva, ComprobanteCheckout receipt) {
        String title = context.getString(R.string.cliente_checkout_confirmado,
                String.format(Locale.US, "%,.2f", receipt.total));
        ClientCheckoutAlerts.show(context, Collections.singletonList(new NotificacionCliente(
                reserva.id, NotificacionCliente.Tipo.CHECKOUT, title, receipt.fecha,
                ClientCheckoutAlerts.isRead(context, reserva.id), reserva.id)));
    }
}
