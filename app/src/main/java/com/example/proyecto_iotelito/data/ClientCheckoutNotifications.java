package com.example.proyecto_iotelito.data;

import android.content.Context;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.model.Reserva;
import com.google.firebase.auth.FirebaseAuth;

/** Avisos al recibir datos remotos con la app abierta o al volver a ingresar; no es entrega push. */
public final class ClientCheckoutNotifications {
    private ClientCheckoutNotifications() { }
    public static void observar(AppCompatActivity activity) {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        String uid = user.getUid();
        java.util.Set<Integer> consultando = new java.util.HashSet<>();
        ReservaRepository.getInstance(activity).observarTodas().observe(activity, rows -> {
            for (Reserva row : rows) {
                if (!CheckoutPolicy.finalizada(row.estado.name()) || notificado(activity, uid, row.id)
                        || !consultando.add(row.id)) continue;
                ClientLocalTasks.run(activity, activity, () -> ReservaRepository.getInstance(activity).comprobante(row.id), receipt -> {
                    consultando.remove(row.id);
                    if (receipt != null && FirebaseAuth.getInstance().getCurrentUser() != null
                            && uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))
                        notificarUnaVez(activity, row, receipt);
                }, () -> consultando.remove(row.id));
            }
        });
    }
    private static boolean notificado(Context context, String uid, int id) {
        return context.getSharedPreferences("checkout_avisos_" + uid, Context.MODE_PRIVATE).getBoolean(String.valueOf(id), false);
    }
    public static void notificarUnaVez(Context context, Reserva reserva, ComprobanteCheckout receipt) {
        com.google.firebase.auth.FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null || notificado(context, user.getUid(), reserva.id)) return;
        com.example.proyecto_iotelito.notification.ClienteNotificationManager.mostrarPagoRealizado(context, reserva, receipt.total);
        context.getSharedPreferences("checkout_avisos_" + user.getUid(), Context.MODE_PRIVATE)
                .edit().putBoolean(String.valueOf(reserva.id), true).apply();
    }
}
