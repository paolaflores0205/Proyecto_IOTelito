package com.example.proyecto_iotelito.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.superadmin.local.AdminAlertEntity;
import com.example.proyecto_iotelito.ui.superadmin.hoteles.DetalleHotelAdminActivity;

public final class SuperadminNotifications {
    public static final String CHANNEL = "superadmin_administration";
    private SuperadminNotifications() { }

    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Administración de hoteles",
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("Hoteles pendientes de asignación de administrador");
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    public static boolean hasPermission(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean publish(Context context, AdminAlertEntity alert) {
        createChannel(context);
        if (!hasPermission(context) || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return false;
        NotificationChannel channel = context.getSystemService(NotificationManager.class).getNotificationChannel(CHANNEL);
        if (channel != null && channel.getImportance() == NotificationManager.IMPORTANCE_NONE) return false;
        Intent intent = new Intent(context, DetalleHotelAdminActivity.class);
        intent.putExtra(DetalleHotelAdminActivity.EXTRA_HOTEL_ID, alert.hotelId);
        intent.putExtra("admin_alert", true);
        PendingIntent pending = TaskStackBuilder.create(context).addNextIntentWithParentStack(intent)
                .getPendingIntent(id(alert.key), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        try {
            NotificationManagerCompat.from(context).notify(id(alert.key),
                    new NotificationCompat.Builder(context, CHANNEL)
                            .setSmallIcon(R.drawable.ic_notifications)
                            .setContentTitle(alert.title).setContentText(alert.message)
                            .setStyle(new NotificationCompat.BigTextStyle().bigText(alert.message))
                            .setContentIntent(pending).setAutoCancel(true).build());
            return true;
        } catch (SecurityException denied) {
            return false;
        }
    }

    public static void cancel(Context context, String key) {
        context.getSystemService(NotificationManager.class).cancel(id(key));
    }

    private static int id(String key) { return key.hashCode() & Integer.MAX_VALUE; }
}
