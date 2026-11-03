package com.example.proyecto_iotelito.data.superadmin;

import android.content.*;
import com.google.firebase.auth.FirebaseAuth;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.example.proyecto_iotelito.data.superadmin.local.AdminAlertEntity;
import com.example.proyecto_iotelito.notification.SuperadminNotifications;
import java.util.*;

/** Condición remota; lectura y deduplicación son preferencias privadas de cada UID/dispositivo. */
public final class RemoteHotelAlerts {
    private RemoteHotelAlerts() { }
    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences("remote_hotel_alerts", Context.MODE_PRIVATE);
    }
    public static List<AdminAlertEntity> sync(Context context, List<ManagedHotel> hotels, boolean notify) {
        List<AdminAlertEntity> rows = new ArrayList<>();
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return rows;
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        SharedPreferences prefs = prefs(context);
        Set<String> previous = new HashSet<>(prefs.getStringSet(uid + ":pending", Collections.emptySet()));
        Set<String> current = new HashSet<>();
        for (ManagedHotel hotel : hotels) {
            SuperadminNotifications.cancel(context, "hotel_pending_" + hotel.id);
            if (!hotel.active || hotel.hasAdministrator()) continue;
            String key = uid + ":hotel:" + hotel.id; current.add(key);
            if (!previous.contains(key)) prefs.edit().putLong(key + ":time", System.currentTimeMillis())
                    .putBoolean(key + ":read", false).putBoolean(key + ":sent", false).apply();
            AdminAlertEntity alert = new AdminAlertEntity(); alert.key = key; alert.hotelId = hotel.id;
            alert.title = "Hotel pendiente de administrador"; alert.message = hotel.name + " necesita un administrador asignado.";
            alert.createdAt = prefs.getLong(key + ":time", System.currentTimeMillis()); alert.read = prefs.getBoolean(key + ":read", false);
            if (notify && !alert.read && !prefs.getBoolean(key + ":sent", false) && SuperadminNotifications.publish(context, alert))
                prefs.edit().putBoolean(key + ":sent", true).apply();
            rows.add(alert);
        }
        for (String key : previous) if (!current.contains(key)) SuperadminNotifications.cancel(context, key);
        prefs.edit().putStringSet(uid + ":pending", current).apply(); return rows;
    }
    public static void read(Context context, AdminAlertEntity alert) {
        prefs(context).edit().putBoolean(alert.key + ":read", true).apply();
        SuperadminNotifications.cancel(context, alert.key);
    }
    public static void cancelSession(Context context, String uid) {
        for (String key : prefs(context).getStringSet(uid + ":pending", Collections.emptySet())) SuperadminNotifications.cancel(context, key);
    }
}
