package com.example.proyecto_iotelito.data.taxista;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Ruta por calles entre dos puntos usando OSRM (OpenStreetMap). Si OSRM no responde, devuelve una
 * línea recta con distancia y tiempo estimados, para que el mapa nunca quede sin ruta.
 */
public final class RutaTaxi {

    /** Servidor público de demostración de OSRM (perfil auto). */
    static final String OSRM = "https://router.project-osrm.org/route/v1/driving/";
    /** Velocidad promedio supuesta para la ruta en línea recta (tráfico urbano). */
    static final double KMH_ESTIMADO = 25;
    /** Se vuelve a pedir la ruta si el taxi avanzó al menos esto... */
    static final double METROS_RECALCULO = 100;
    /** ...o si pasó este tiempo y se movió algo (evita pedir rutas con el auto parado). */
    static final long MS_RECALCULO = 30_000;
    static final double METROS_MINIMO = 20;

    private RutaTaxi() { }

    /** Resultado: puntos {lat, lng}, metros, segundos y si viene de OSRM o es la línea recta. */
    public static final class Ruta {
        public final List<double[]> puntos;
        public final double metros, segundos;
        public final boolean porCalles;

        Ruta(List<double[]> puntos, double metros, double segundos, boolean porCalles) {
            this.puntos = Collections.unmodifiableList(puntos);
            this.metros = metros;
            this.segundos = segundos;
            this.porCalles = porCalles;
        }

        /** "3.2 km · 9 min", o "≈ 3.2 km en línea recta" si OSRM no respondió. */
        public String resumen() {
            String km = metros < 1000
                    ? Math.round(metros) + " m"
                    : String.format(Locale.US, "%.1f km", metros / 1000);
            long min = Math.max(1, Math.round(segundos / 60));
            return porCalles ? km + " · " + min + " min" : "≈ " + km + " · " + min + " min (en línea recta)";
        }
    }

    /** Coordenadas {lat, lng} del aeropuerto por su código de la API, o por su nombre si no hay código. */
    @Nullable
    public static double[] aeropuerto(@Nullable String codigo, @Nullable String nombre) {
        String c = codigo == null ? "" : codigo.trim().toUpperCase(Locale.ROOT);
        String n = sinTildes(nombre);
        if (c.equals("LIM") || n.contains("jorge chavez") || n.contains("callao")) return new double[] {-12.0219, -77.1143};
        if (c.equals("CUZ") || n.contains("velasco astete") || n.contains("cusco")) return new double[] {-13.5357, -71.9388};
        if (c.equals("AQP") || n.contains("rodriguez ballon") || n.contains("arequipa")) return new double[] {-16.3411, -71.5830};
        if (c.equals("TRU") || n.contains("martinez de pinillos") || n.contains("trujillo")) return new double[] {-8.0814, -79.1088};
        if (c.equals("PIO") || n.contains("renan elias") || n.contains("pisco")) return new double[] {-13.7449, -76.2203};
        return null;
    }

    static String url(double latDesde, double lngDesde, double latHasta, double lngHasta) {
        return OSRM + String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f", lngDesde, latDesde, lngHasta, latHasta)
                + "?overview=full&geometries=geojson";
    }

    /** Lee la respuesta de OSRM; null si no trae una ruta válida. */
    @Nullable
    static Ruta leer(String json) {
        try {
            JSONObject raiz = new JSONObject(json);
            if (!"Ok".equals(raiz.optString("code"))) return null;
            JSONArray rutas = raiz.optJSONArray("routes");
            JSONObject ruta = rutas == null ? null : rutas.optJSONObject(0);
            if (ruta == null) return null;
            JSONArray coords = ruta.getJSONObject("geometry").getJSONArray("coordinates");
            List<double[]> puntos = new ArrayList<>(coords.length());
            for (int i = 0; i < coords.length(); i++) {
                JSONArray p = coords.getJSONArray(i);
                puntos.add(new double[] {p.getDouble(1), p.getDouble(0)});
            }
            if (puntos.size() < 2) return null;
            return new Ruta(puntos, ruta.getDouble("distance"), ruta.getDouble("duration"), true);
        } catch (JSONException e) {
            return null;
        }
    }

    static Ruta recta(double latDesde, double lngDesde, double latHasta, double lngHasta) {
        List<double[]> puntos = new ArrayList<>();
        puntos.add(new double[] {latDesde, lngDesde});
        puntos.add(new double[] {latHasta, lngHasta});
        double metros = metros(latDesde, lngDesde, latHasta, lngHasta);
        return new Ruta(puntos, metros, metros / (KMH_ESTIMADO * 1000 / 3600), false);
    }

    /** Pide la ruta a OSRM (máximo ~8 s); si falla, devuelve la línea recta. */
    @WorkerThread
    public static Ruta calcular(double latDesde, double lngDesde, double latHasta, double lngHasta) {
        HttpURLConnection conexion = null;
        try {
            conexion = (HttpURLConnection) new URL(url(latDesde, lngDesde, latHasta, lngHasta)).openConnection();
            conexion.setConnectTimeout(4000);
            conexion.setReadTimeout(4000);
            conexion.setRequestProperty("User-Agent", "IoTelito (Android)");
            if (conexion.getResponseCode() == 200) {
                try (InputStream in = conexion.getInputStream()) {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    for (int n; (n = in.read(buffer)) > 0; ) out.write(buffer, 0, n);
                    Ruta ruta = leer(out.toString(StandardCharsets.UTF_8.name()));
                    if (ruta != null) return ruta;
                }
            }
        } catch (Exception ignored) {
            // Sin internet o OSRM caído: se usa la línea recta.
        } finally {
            if (conexion != null) conexion.disconnect();
        }
        return recta(latDesde, lngDesde, latHasta, lngHasta);
    }

    /**
     * Si conviene volver a pedir la ruta: cambió el destino, nunca se calculó, el taxi avanzó
     * 100 m, o pasaron 30 s y se movió al menos 20 m.
     */
    public static boolean recalcular(@Nullable double[] ultimoDesde, long ultimoMs, boolean mismoDestino,
                                     double lat, double lng, long ahoraMs) {
        if (ultimoDesde == null || !mismoDestino) return true;
        double movido = metros(ultimoDesde[0], ultimoDesde[1], lat, lng);
        return movido >= METROS_RECALCULO || (ahoraMs - ultimoMs >= MS_RECALCULO && movido >= METROS_MINIMO);
    }

    /** Distancia en metros entre dos coordenadas (fórmula del haversine). */
    public static double metros(double lat1, double lng1, double lat2, double lng2) {
        double r = 6_371_000, dLat = Math.toRadians(lat2 - lat1), dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * r * Math.asin(Math.sqrt(a));
    }

    private static String sinTildes(@Nullable String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }
}
