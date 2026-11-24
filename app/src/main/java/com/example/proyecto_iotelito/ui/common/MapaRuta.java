package com.example.proyecto_iotelito.ui.common;

import android.content.Context;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.taxista.RutaTaxi;
import com.google.firebase.firestore.FirebaseFirestore;

import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Ruta estilo Uber sobre un mapa OSMDroid: marca el hotel (recojo) y el aeropuerto, y traza el
 * camino por calles desde el taxi hasta el destino actual (OSRM; si falla, línea punteada).
 * Recalcula sola cuando el taxi avanza, sin pedir rutas en cada lectura del GPS.
 */
public final class MapaRuta {

    private final Context context;
    private final MapView mapa;
    private final Handler principal = new Handler(Looper.getMainLooper());
    private final ExecutorService hilo = Executors.newSingleThreadExecutor();
    private final Polyline linea;
    private Marker marcaHotel, marcaAeropuerto;
    private GeoPoint hotel, aeropuerto, taxi;
    private boolean alHotel = true, encuadrado, cerrado, calculando;
    private double[] ultimoDesde;
    private GeoPoint ultimoDestino;
    private long ultimoMs;
    private int hotelCargado = -1;
    @Nullable private String ultimoResumen;
    @Nullable private Consumer<String> alResumen;

    public MapaRuta(Context context, MapView mapa) {
        this.context = context;
        this.mapa = mapa;
        linea = new Polyline(mapa);
        Paint pintura = linea.getOutlinePaint();
        pintura.setColor(ContextCompat.getColor(context, R.color.io_teal));
        pintura.setStrokeWidth(12f);
        pintura.setStrokeCap(Paint.Cap.ROUND);
        pintura.setStrokeJoin(Paint.Join.ROUND);
        linea.setVisible(false);
        mapa.getOverlays().add(0, linea);
    }

    /** Texto "3.2 km · 9 min" de la ruta actual, cada vez que se recalcula. */
    public void alResumen(@Nullable Consumer<String> callback) {
        alResumen = callback;
        if (callback != null && ultimoResumen != null) callback.accept(ultimoResumen);
    }

    /** Destino actual: el hotel (ir a recoger) o el aeropuerto (traslado). */
    public void haciaHotel(boolean alHotel) {
        if (this.alHotel == alHotel) return;
        this.alHotel = alHotel;
        encuadrado = false;
        ultimoResumen = null;
        actualizar();
    }

    /** Busca las coordenadas del hotel en Firestore (hotels/{id}) y las del aeropuerto en la tabla fija. */
    public void lugares(int hotelId, @Nullable String codigoAeropuerto, @Nullable String nombreAeropuerto) {
        double[] a = RutaTaxi.aeropuerto(codigoAeropuerto, nombreAeropuerto);
        if (a != null) {
            aeropuerto = new GeoPoint(a[0], a[1]);
            marcaAeropuerto = marca(marcaAeropuerto, aeropuerto, R.string.mapa_ruta_aeropuerto, R.color.io_navy);
        }
        actualizar();
        if (hotelId <= 0 || hotelId == hotelCargado) return;
        hotelCargado = hotelId;
        FirebaseFirestore.getInstance().collection("hotels").document(String.valueOf(hotelId)).get()
                .addOnSuccessListener(doc -> {
                    Double lat = doc.getDouble("latitude"), lng = doc.getDouble("longitude");
                    if (cerrado || lat == null || lng == null) return;
                    hotel = new GeoPoint(lat, lng);
                    marcaHotel = marca(marcaHotel, hotel, R.string.mapa_ruta_hotel, R.color.io_teal);
                    encuadrado = false;
                    actualizar();
                });
    }

    /** Nueva posición del taxi (GPS del taxista o la que publica la API al cliente). */
    public void taxi(double lat, double lng) {
        taxi = new GeoPoint(lat, lng);
        actualizar();
    }

    public void cerrar() {
        cerrado = true;
        hilo.shutdownNow();
        principal.removeCallbacksAndMessages(null);
    }

    private void actualizar() {
        if (cerrado) return;
        GeoPoint destino = alHotel ? hotel : aeropuerto;
        // Sin GPS del taxi, el traslado se dibuja desde el hotel.
        GeoPoint desde = taxi != null ? taxi : (alHotel ? null : hotel);
        if (destino == null || desde == null) {
            linea.setVisible(false);
            encuadrar(null);
            mapa.invalidate();
            return;
        }
        boolean mismoDestino = destino.equals(ultimoDestino);
        long ahora = SystemClock.elapsedRealtime();
        if (calculando || !RutaTaxi.recalcular(ultimoDesde, ultimoMs, mismoDestino,
                desde.getLatitude(), desde.getLongitude(), ahora)) return;
        calculando = true;
        ultimoDesde = new double[] {desde.getLatitude(), desde.getLongitude()};
        ultimoDestino = destino;
        ultimoMs = ahora;
        hilo.execute(() -> {
            RutaTaxi.Ruta ruta = RutaTaxi.calcular(desde.getLatitude(), desde.getLongitude(),
                    destino.getLatitude(), destino.getLongitude());
            principal.post(() -> {
                calculando = false;
                if (!cerrado) dibujar(ruta, destino);
            });
        });
    }

    private void dibujar(RutaTaxi.Ruta ruta, GeoPoint destino) {
        if (!destino.equals(alHotel ? hotel : aeropuerto)) {   // cambió de etapa mientras calculaba
            ultimoDesde = null;
            actualizar();
            return;
        }
        List<GeoPoint> puntos = new ArrayList<>(ruta.puntos.size());
        for (double[] p : ruta.puntos) puntos.add(new GeoPoint(p[0], p[1]));
        linea.setPoints(puntos);
        linea.getOutlinePaint().setPathEffect(ruta.porCalles ? null : new DashPathEffect(new float[] {30f, 20f}, 0));
        linea.getOutlinePaint().setAlpha(ruta.porCalles ? 255 : 140);
        linea.setVisible(true);
        encuadrar(puntos);
        mapa.invalidate();
        ultimoResumen = ruta.resumen();
        if (alResumen != null) alResumen.accept(ultimoResumen);
    }

    /** Encuadra una sola vez por etapa la ruta (o los puntos conocidos), sin pelear con el usuario. */
    private void encuadrar(@Nullable List<GeoPoint> ruta) {
        if (encuadrado) return;
        List<GeoPoint> puntos = new ArrayList<>();
        if (ruta != null) puntos.addAll(ruta);
        for (GeoPoint p : new GeoPoint[] {taxi, alHotel ? hotel : aeropuerto}) if (p != null) puntos.add(p);
        if (puntos.size() < 2) return;
        encuadrado = ruta != null;
        BoundingBox caja = BoundingBox.fromGeoPoints(puntos);
        mapa.post(() -> {
            if (!cerrado) mapa.zoomToBoundingBox(caja.increaseByScale(1.3f), false, 64);
        });
    }

    private Marker marca(@Nullable Marker actual, GeoPoint punto, int titulo, int color) {
        Marker m = actual;
        if (m == null) {
            m = new Marker(mapa);
            m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            m.setTitle(context.getString(titulo));
            Drawable icono = ContextCompat.getDrawable(context, R.drawable.ic_location_pin);
            if (icono != null) {
                icono = icono.mutate();
                icono.setTint(ContextCompat.getColor(context, color));
                m.setIcon(icono);
            }
            mapa.getOverlays().add(m);
        }
        m.setPosition(punto);
        return m;
    }
}
