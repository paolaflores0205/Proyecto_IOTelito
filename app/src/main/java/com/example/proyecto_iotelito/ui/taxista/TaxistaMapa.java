package com.example.proyecto_iotelito.ui.taxista;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.example.proyecto_iotelito.BuildConfig;
import com.example.proyecto_iotelito.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.util.concurrent.TimeUnit;

/**
 * Mapa real (OpenStreetMap) para las pantallas del taxista: muestra su ubicación actual y la
 * sigue mientras la pantalla está visible. Si no hay permiso de ubicación, queda centrado en Lima.
 */
final class TaxistaMapa implements DefaultLifecycleObserver {

    private static final GeoPoint LIMA = new GeoPoint(-12.0464, -77.0428);
    private static final long INTERVALO_MS = 5000;

    private final AppCompatActivity activity;
    private final MapView mapa;
    private final FusedLocationProviderClient ubicacion;
    private Marker taxi;
    private boolean centrado;

    private final LocationCallback alMoverse = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult resultado) {
            Location ultima = resultado.getLastLocation();
            if (ultima != null) mostrar(ultima);
        }
    };

    /**
     * Monta el mapa con id {@code mapa_taxista} del layout ya inflado. Con {@code interactivo}
     * en false el mapa es solo una vista previa (no roba los gestos al scroll de la pantalla).
     */
    static void montar(AppCompatActivity activity, boolean interactivo) {
        MapView mapa = activity.findViewById(R.id.mapa_taxista);
        if (mapa == null) return;
        activity.getLifecycle().addObserver(new TaxistaMapa(activity, mapa, interactivo));
    }

    private TaxistaMapa(AppCompatActivity activity, MapView mapa, boolean interactivo) {
        this.activity = activity;
        this.mapa = mapa;
        this.ubicacion = LocationServices.getFusedLocationProviderClient(activity);
        Configuration.getInstance().setUserAgentValue("IoTelito/" + BuildConfig.VERSION_NAME
                + " (Android; https://github.com/paolaflores0205/Proyecto_IOTelito)");
        Configuration.getInstance().setExpirationExtendedDuration(TimeUnit.DAYS.toMillis(7));
        mapa.setTileSource(new XYTileSource("OpenStreetMap", 0, 19, 256, ".png",
                new String[] {"https://tile.openstreetmap.org/"},
                activity.getString(R.string.taxi_api_attribution)));
        mapa.setMultiTouchControls(interactivo);
        mapa.getZoomController().setVisibility(
                org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER);
        if (!interactivo) mapa.setOnTouchListener((v, evento) -> true);
        mapa.getController().setZoom(16.0);
        mapa.getController().setCenter(LIMA);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onResume(@NonNull LifecycleOwner owner) {
        mapa.onResume();
        if (!TaxistaLocationService.permiso(activity)) return;
        ubicacion.getLastLocation().addOnSuccessListener(activity, ultima -> {
            if (ultima != null) mostrar(ultima);
        });
        ubicacion.requestLocationUpdates(
                new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, INTERVALO_MS).build(),
                alMoverse, Looper.getMainLooper());
    }

    @Override
    public void onPause(@NonNull LifecycleOwner owner) {
        ubicacion.removeLocationUpdates(alMoverse);
        mapa.onPause();
    }

    @Override
    public void onDestroy(@NonNull LifecycleOwner owner) {
        mapa.onDetach();
    }

    private void mostrar(Location lugar) {
        GeoPoint punto = new GeoPoint(lugar.getLatitude(), lugar.getLongitude());
        if (taxi == null) {
            taxi = new Marker(mapa);
            taxi.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            taxi.setTitle(activity.getString(R.string.taxista_mapa_tu_ubicacion));
            Drawable icono = ContextCompat.getDrawable(activity, R.drawable.ic_taxi);
            if (icono != null) {
                icono = icono.mutate();
                icono.setTint(ContextCompat.getColor(activity, R.color.io_teal));
                taxi.setIcon(icono);
            }
            mapa.getOverlays().add(taxi);
        }
        taxi.setPosition(punto);
        if (!centrado) {
            mapa.getController().setCenter(punto);
            centrado = true;
        }
        mapa.invalidate();
    }
}
