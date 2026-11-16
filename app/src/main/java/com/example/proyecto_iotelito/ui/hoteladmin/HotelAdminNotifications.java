package com.example.proyecto_iotelito.ui.hoteladmin;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;
import com.example.proyecto_iotelito.ui.hoteladmin.reservas.CobroCheckoutActivity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    public static void showRemoteCheckouts(Context context, String uid, int hotelId, List<ReservaAdmin> rows) {
        String tag = "hotel-checkout:" + uid + ":" + hotelId;
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        Set<Integer> pending = new HashSet<>();
        for (ReservaAdmin row : rows) {
            if (row.hotelId == hotelId && row.estado == ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE) {
                pending.add(row.id);
            }
        }
        for (android.service.notification.StatusBarNotification item : manager.getActiveNotifications()) {
            if (tag.equals(item.getTag()) && !pending.contains(item.getId())) manager.cancel(tag, item.getId());
        }
        if (!hasPermission(context) || !manager.areNotificationsEnabled()) return;
        android.content.SharedPreferences prefs = context.getSharedPreferences("hoteladmin_alerts", Context.MODE_PRIVATE);
        for (int id : pending) {
            String key = tag + ":" + id;
            if (prefs.getBoolean(key, false)) continue;
            Intent intent = new Intent(context, CobroCheckoutActivity.class)
                    .setData(Uri.parse("iotelito://admin-checkout/" + hotelId + "/" + id))
                    .putExtra(CobroCheckoutActivity.EXTRA_RESERVA_ID, id);
            PendingIntent link = PendingIntent.getActivity(context, id, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            try {
                manager.notify(tag, id, new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_notifications)
                        .setContentTitle(context.getString(R.string.hoteladmin_notif_checkout_titulo))
                        .setContentText(context.getString(R.string.hoteladmin_checkout_aviso_remoto))
                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                        .setContentIntent(link).setAutoCancel(true).build());
                prefs.edit().putBoolean(key, true).apply();
            } catch (SecurityException ignored) {
                // El permiso pudo cambiar mientras se publicaba el aviso.
            }
        }
    }

    public static void cancelRemoteSession(Context context, String uid) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        for (android.service.notification.StatusBarNotification item : manager.getActiveNotifications()) {
            if (item.getTag() != null && item.getTag().startsWith("hotel-checkout:" + uid + ":")) {
                manager.cancel(item.getTag(), item.getId());
            }
        }
    }
}
