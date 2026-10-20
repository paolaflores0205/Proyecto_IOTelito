package com.example.proyecto_iotelito.ui.hoteladmin;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;
import com.example.proyecto_iotelito.ui.hoteladmin.reservas.CobroCheckoutActivity;

/** Avisa una sola vez por cada checkout pendiente de gestionar en este dispositivo. */
public final class HotelAdminNotifications {
    private static final String CHANNEL_ID = "hoteladmin_checkout";

    private HotelAdminNotifications() { }

    public static void createChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                context.getString(R.string.hoteladmin_notif_canal),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.hoteladmin_notif_canal_desc));
        manager.createNotificationChannel(channel);
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void showPendingCheckouts(Context context, java.util.List<ReservaAdmin> reservas) {
        if (!hasPermission(context)) return;
        createChannel(context);
        android.content.SharedPreferences prefs = context.getSharedPreferences(
                "hoteladmin_alerts", Context.MODE_PRIVATE);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        for (ReservaAdmin reserva : reservas) {
            if (reserva.estado != ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE) {
                manager.cancel(reserva.id);
                continue;
            }
            if (prefs.getBoolean("checkout_" + reserva.id, false)) continue;

            Intent intent = new Intent(context, CobroCheckoutActivity.class);
            intent.putExtra(CobroCheckoutActivity.EXTRA_RESERVA_ID, reserva.id);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, reserva.id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            try {
                manager.notify(reserva.id, new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notifications)
                        .setContentTitle(context.getString(R.string.hoteladmin_notif_checkout_titulo))
                        .setContentText(context.getString(R.string.hoteladmin_notif_checkout_texto,
                                reserva.huespedNombre))
                        .setContentIntent(pendingIntent)
                        .setAutoCancel(true)
                        .build());
                prefs.edit().putBoolean("checkout_" + reserva.id, true).apply();
            } catch (SecurityException ignored) {
                // Si se revoca el permiso, queda pendiente para el siguiente ingreso.
            }
        }
    }

    public static void cancelCheckout(Context context, int reservaId) {
        context.getSystemService(NotificationManager.class).cancel(reservaId);
    }
}
