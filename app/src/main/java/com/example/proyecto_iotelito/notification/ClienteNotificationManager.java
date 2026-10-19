package com.example.proyecto_iotelito.notification;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.NotificacionRepository;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.NotificacionCliente;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.ui.reservas.DetalleReservaActivity;

import java.util.Locale;

/** Crea el canal y publica las notificaciones del flujo de reservas del cliente. */
public final class ClienteNotificationManager {

    public static final String CHANNEL_RESERVAS = "reservas_cliente";

    private ClienteNotificationManager() { }

    public static void crearCanal(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_RESERVAS,
                context.getString(R.string.notif_canal_reservas),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.notif_canal_reservas_descripcion));
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    public static boolean tienePermiso(Context context) {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void mostrarReservaConfirmada(Context context, Reserva reserva) {
        Hotel hotel = SampleData.findById(reserva.hotelId);
        publicar(
                context,
                10_000 + reserva.id,
                NotificacionCliente.Tipo.RESERVA,
                reserva.id,
                context.getString(R.string.notif_reserva_confirmada_titulo),
                context.getString(R.string.notif_reserva_confirmada_texto, hotel.name, reserva.rangoFechasTexto()));
    }

    public static void mostrarPagoRealizado(Context context, Reserva reserva) {
        publicar(
                context,
                20_000 + reserva.id,
                NotificacionCliente.Tipo.PAGO,
                reserva.id,
                context.getString(R.string.notif_pago_realizado_titulo),
                context.getString(
                        R.string.notif_pago_realizado_texto,
                        String.format(Locale.US, "%,.0f", reserva.precioTotal),
                        reserva.codigo));
    }

    @SuppressLint("MissingPermission")
    private static void publicar(Context context, int notificationId,
                                 NotificacionCliente.Tipo tipo, int reservaId,
                                 String titulo, String texto) {
        NotificacionRepository.getInstance(context).guardarSiNoExiste(
                notificationId, tipo, texto, reservaId);
        if (!tienePermiso(context)) {
            return;
        }

        Intent intent = new Intent(context, DetalleReservaActivity.class);
        intent.putExtra(DetalleReservaActivity.EXTRA_RESERVA_ID, reservaId);
        intent.putExtra(DetalleReservaActivity.EXTRA_NOTIFICACION_ID, notificationId);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notification = new NotificationCompat.Builder(context, CHANNEL_RESERVAS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification.build());
        } catch (SecurityException ignored) {
            // El permiso puede revocarse entre la comprobación y esta llamada.
        }
    }
}
