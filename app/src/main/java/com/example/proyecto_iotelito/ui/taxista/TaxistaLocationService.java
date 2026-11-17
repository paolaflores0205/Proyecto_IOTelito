package com.example.proyecto_iotelito.ui.taxista;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.data.taxiapi.*;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.google.android.gms.location.*;
import com.google.android.gms.tasks.CancellationTokenSource;

import java.util.concurrent.*;

/** Ubicación visible para el usuario y limitada al viaje activo. No usa ubicación de fondo. */
public final class TaxistaLocationService extends Service {
    private static final String CHANNEL = "taxista_ubicacion";
    private static volatile int runningId = -1;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private CancellationTokenSource cancellation;
    private int serviceId = -1;
    private boolean busy, closed;

    public static boolean permiso(Context c) {
        return ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    public static void start(Context c, int id) {
        if (id < 1 || runningId == id || !permiso(c)) return;
        ContextCompat.startForegroundService(
                c, new Intent(c, TaxistaLocationService.class).putExtra("servicio", id));
    }

    public static void stop(Context c) {
        c.stopService(new Intent(c, TaxistaLocationService.class));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || !permiso(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        serviceId = intent.getIntExtra("servicio", -1);
        if (serviceId < 1) {
            stopSelf();
            return START_NOT_STICKY;
        }
        runningId = serviceId;
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(
                new NotificationChannel(
                        CHANNEL,
                        getString(R.string.taxi_api_location_channel),
                        NotificationManager.IMPORTANCE_LOW));
        Intent open = new Intent(this, TaxistaMainActivity.class);
        PendingIntent pending =
                PendingIntent.getActivity(
                        this,
                        0,
                        open,
                        PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification =
                new NotificationCompat.Builder(this, CHANNEL)
                        .setSmallIcon(R.drawable.ic_taxi)
                        .setContentTitle(getString(R.string.taxi_api_location_active))
                        .setContentIntent(pending)
                        .setOngoing(true)
                        .build();
        startForeground(9451, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        main.removeCallbacks(tick);
        main.post(tick);
        return START_NOT_STICKY;
    }

    private final Runnable tick =
            new Runnable() {
                @Override
                public void run() {
                    if (closed) return;
                    com.example.proyecto_iotelito.model.auth.AuthenticatedUser user =
                            SessionRepository.obtener(TaxistaLocationService.this);
                    if (user == null
                            || user.getRole() != UserRole.TAXISTA
                            || !permiso(TaxistaLocationService.this)) {
                        stopSelf();
                        return;
                    }
                    if (busy) {
                        main.postDelayed(this, 15000);
                        return;
                    }
                    busy = true;
                    cancellation = new CancellationTokenSource();
                    final int id = serviceId;
                    final TaxiApi api = TaxiApis.get(TaxistaLocationService.this);
                    try {
                        LocationServices.getFusedLocationProviderClient(TaxistaLocationService.this)
                                .getCurrentLocation(
                                        new CurrentLocationRequest.Builder()
                                                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                                                .setMaxUpdateAgeMillis(15000)
                                                .setDurationMillis(10000)
                                                .build(),
                                        cancellation.getToken())
                                .addOnSuccessListener(
                                        location -> {
                                            if (location == null) {
                                                busy = false;
                                                notifyError(
                                                        "Activa la ubicación del teléfono para"
                                                                + " compartirla.");
                                                return;
                                            }
                                            io.execute(
                                                    () -> {
                                                        try {
                                                            if (closed) return;
                                                            api.enviarUbicacion(
                                                                    id,
                                                                    location.getLatitude(),
                                                                    location.getLongitude(),
                                                                    location.hasAccuracy()
                                                                            ? (double)
                                                                                    location
                                                                                            .getAccuracy()
                                                                            : null);
                                                        } catch (TaxiApiException e) {
                                                            if (e.status == 409
                                                                    || e.status == 401
                                                                    || e.status == 403
                                                                    || e.status == 404)
                                                                main.post(() -> stopSelf());
                                                            else notifyError(e.getMessage());
                                                        } catch (Exception e) {
                                                            notifyError(TaxiTasks.message(e));
                                                        } finally {
                                                            main.post(() -> busy = false);
                                                        }
                                                    });
                                        })
                                .addOnFailureListener(
                                        error -> {
                                            busy = false;
                                            notifyError(
                                                    "No se pudo obtener la ubicación del"
                                                            + " teléfono.");
                                        });
                    } catch (SecurityException e) {
                        busy = false;
                        stopSelf();
                        return;
                    }
                    main.postDelayed(this, 15000);
                }
            };

    private void notifyError(String error) {
        main.post(
                () -> {
                    if (!closed)
                        getSystemService(NotificationManager.class)
                                .notify(
                                        9451,
                                        new NotificationCompat.Builder(this, CHANNEL)
                                                .setSmallIcon(R.drawable.ic_taxi)
                                                .setContentTitle("Ubicación del servicio")
                                                .setContentText(error)
                                                .setOngoing(true)
                                                .build());
                });
    }

    @Override
    public void onDestroy() {
        closed = true;
        runningId = -1;
        main.removeCallbacksAndMessages(null);
        if (cancellation != null) cancellation.cancel();
        io.shutdownNow();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
