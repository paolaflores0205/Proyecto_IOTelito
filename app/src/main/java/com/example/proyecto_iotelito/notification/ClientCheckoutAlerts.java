package com.example.proyecto_iotelito.notification;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.model.NotificacionCliente;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.example.proyecto_iotelito.ui.reservas.DetalleReservaActivity;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/** Deriva los avisos de cierre de los comprobantes del huésped autenticado. */
public final class ClientCheckoutAlerts {
    public static final String EXTRA_CHECKOUT_ALERT = "extra_checkout_alert";

    private ClientCheckoutAlerts() { }

    private static String clientUid(Context context) {
        AuthenticatedUser user = SessionRepository.obtener(context);
        return user != null && user.getRole() == UserRole.CLIENT ? user.getUid() : null;
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("checkout_alerts_client", Context.MODE_PRIVATE);
    }

    private static String tag(String uid) { return "client-checkout:" + uid; }
    private static String key(String uid, int id, String flag) { return tag(uid) + ":" + id + ":" + flag; }

    public static LiveData<List<NotificacionCliente>> observe(Context context) {
        Context app = context.getApplicationContext();
        String uid = clientUid(app);
        return new LiveData<List<NotificacionCliente>>(Collections.emptyList()) {
            private ListenerRegistration registration;
            private int generation;

            @Override protected void onActive() {
                if (uid == null || !uid.equals(clientUid(app))) return;
                int ticket = ++generation;
                registration = FirebaseFirestore.getInstance().collection("hotelCheckouts")
                        .whereEqualTo("clientUid", uid)
                        .addSnapshotListener(MetadataChanges.INCLUDE, (result, error) -> {
                            if (ticket != generation || !uid.equals(clientUid(app))) return;
                            if (error != null) {
                                Log.w("ClientCheckoutAlerts", "No se pudieron leer los comprobantes", error);
                                return;
                            }
                            if (result == null || result.getMetadata().isFromCache()) return;
                            List<NotificacionCliente> notices = new ArrayList<>();
                            for (DocumentSnapshot doc : result) {
                                if (doc.getMetadata().hasPendingWrites()) continue;
                                Timestamp time = doc.getTimestamp("createdAt");
                                Number total = doc.get("total") instanceof Number ? (Number) doc.get("total") : null;
                                if (time == null || total == null || !Double.isFinite(total.doubleValue())) continue;
                                try {
                                    int id = Integer.parseInt(doc.getId());
                                    if (id <= 0) continue;
                                    notices.add(new NotificacionCliente(id, NotificacionCliente.Tipo.CHECKOUT,
                                            app.getString(R.string.cliente_checkout_confirmado,
                                                    String.format(Locale.US, "%,.2f", total.doubleValue())),
                                            time.toDate().getTime(), isRead(app, id), id));
                                } catch (NumberFormatException ignored) {
                                    // Los comprobantes válidos usan el ID numérico de su reserva.
                                }
                            }
                            setValue(notices);
                        });
            }

            @Override protected void onInactive() {
                generation++;
                if (registration != null) registration.remove();
                registration = null;
                setValue(Collections.emptyList());
            }
        };
    }

    public static boolean isRead(Context context, int reservation) {
        String uid = clientUid(context);
        return uid != null && preferences(context).getBoolean(key(uid, reservation, "read"), false);
    }

    public static void markRead(Context context, int reservation) {
        String uid = clientUid(context);
        if (uid == null || reservation <= 0) return;
        preferences(context).edit().putBoolean(key(uid, reservation, "read"), true).apply();
        context.getSystemService(NotificationManager.class).cancel(tag(uid), reservation);
    }

    public static Intent reservationIntent(Context context, int reservation) {
        return new Intent(context, DetalleReservaActivity.class)
                .setData(Uri.parse("iotelito://client-checkout/" + reservation))
                .putExtra(DetalleReservaActivity.EXTRA_RESERVA_ID, reservation)
                .putExtra(EXTRA_CHECKOUT_ALERT, true);
    }

    public static void show(Context context, List<NotificacionCliente> notices) {
        String uid = clientUid(context);
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (uid == null || !ClienteNotificationManager.tienePermiso(context) || !manager.areNotificationsEnabled()) return;
        ClienteNotificationManager.crearCanal(context);
        SharedPreferences prefs = preferences(context);
        for (NotificacionCliente notice : notices) {
            String sent = key(uid, notice.id, "sent");
            if (isRead(context, notice.id) || prefs.getBoolean(sent, false)) continue;
            PendingIntent link = PendingIntent.getActivity(context, notice.id,
                    reservationIntent(context, notice.reservaId), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            try {
                manager.notify(tag(uid), notice.id, new NotificationCompat.Builder(context, ClienteNotificationManager.CHANNEL_RESERVAS)
                        .setSmallIcon(R.drawable.ic_notifications)
                        .setContentTitle(context.getString(R.string.cliente_checkout_titulo))
                        .setContentText(notice.titulo)
                        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                        .setContentIntent(link).setAutoCancel(true).build());
                prefs.edit().putBoolean(sent, true).apply();
            } catch (SecurityException ignored) {
                // El comprobante sigue en la bandeja si se revoca el permiso.
            }
        }
    }

    public static void cancelSession(Context context, String uid) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        for (android.service.notification.StatusBarNotification item : manager.getActiveNotifications()) {
            if (tag(uid).equals(item.getTag())) manager.cancel(item.getTag(), item.getId());
        }
    }
}
