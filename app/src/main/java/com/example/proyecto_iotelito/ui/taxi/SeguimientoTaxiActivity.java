package com.example.proyecto_iotelito.ui.taxi;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.data.TaxiRepository;
import com.example.proyecto_iotelito.model.auth.UserRole;
import com.example.proyecto_iotelito.ui.taxista.ChatPasajeroTaxistaActivity;

import org.json.JSONObject;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

public class SeguimientoTaxiActivity extends TaxiServiceScreen {
    private MapView map;
    private Marker driver;
    private boolean centered;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        Configuration.getInstance()
                .setUserAgentValue(
                        "IoTelito/"
                                + com.example.proyecto_iotelito.BuildConfig.VERSION_NAME
                                + " (Android;"
                                + " https://github.com/paolaflores0205/Proyecto_IOTelito)");
        Configuration.getInstance()
                .setExpirationExtendedDuration(java.util.concurrent.TimeUnit.DAYS.toMillis(7));
        setContentView(R.layout.activity_seguimiento_taxi);
        ((TextView) findViewById(R.id.tv_title)).setText(R.string.taxi_api_map_title);
        map = findViewById(R.id.taxi_map);
        map.setTileSource(
                new org.osmdroid.tileprovider.tilesource.XYTileSource(
                        "OpenStreetMap",
                        0,
                        19,
                        256,
                        ".png",
                        new String[] {"https://tile.openstreetmap.org/"},
                        getString(R.string.taxi_api_attribution)));
        map.setMultiTouchControls(true);
        map.getController().setZoom(15.0);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_ver_qr)
                .setOnClickListener(v -> startActivity(intent(CodigoQrTaxiActivity.class)));
        findViewById(R.id.taxi_map_rate)
                .setOnClickListener(v -> startActivity(intent(CalificarTaxiActivity.class)));
        observeService();
    }

    protected void render(JSONObject s) {
        JSONObject p = s.optJSONObject("taxista"), u = s.optJSONObject("ubicacion");
        String info = s.optString("estado").replace('_', ' ');
        if (p != null) {
            JSONObject car = p.optJSONObject("vehiculo");
            info +=
                    "\n"
                            + p.optString("nombres")
                            + " "
                            + p.optString("apellidos")
                            + "\n"
                            + car.optString("marca")
                            + " "
                            + car.optString("modelo")
                            + " · "
                            + car.optString("placa")
                            + "\nValoración: "
                            + p.optDouble("calificacion");
        }
        if (u == null) info += "\n" + getString(R.string.taxi_api_wait_location);
        else {
            GeoPoint point = new GeoPoint(u.optDouble("lat"), u.optDouble("lng"));
            if (driver == null) {
                driver = new Marker(map);
                driver.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                driver.setTitle("Tu taxista");
                android.graphics.drawable.Drawable icon =
                        androidx.core.content.ContextCompat.getDrawable(this, R.drawable.ic_taxi);
                if (icon != null) icon.setTint(getColor(R.color.io_teal));
                driver.setIcon(icon);
                map.getOverlays().add(driver);
            }
            driver.setPosition(point);
            if (!centered) {
                map.getController().setCenter(point);
                centered = true;
            }
            map.invalidate();
            try {
                info +=
                        "\nActualizada a las "
                                + java.time.Instant.parse(u.optString("registradaEn"))
                                        .atZone(java.time.ZoneId.of("America/Lima"))
                                        .format(
                                                java.time.format.DateTimeFormatter.ofPattern(
                                                        "HH:mm:ss"));
            } catch (Exception ignored) {
                info += "\nUbicación recibida";
            }
        }
        ((TextView) findViewById(R.id.taxi_map_info)).setText(info);
        boolean done = "FINALIZADO".equals(s.optString("estado"));
        findViewById(R.id.taxi_map_rate).setVisibility(done ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_ver_qr).setEnabled(p != null && !done);
        View chat = findViewById(R.id.taxi_map_chat);
        var usuario = SessionRepository.obtener(this);
        boolean cliente = usuario != null && usuario.getRole() == UserRole.CLIENT;
        chat.setVisibility(p != null && cliente ? View.VISIBLE : View.GONE);
        if (p != null && cliente) {
            String nombre = (p.optString("nombres") + " " + p.optString("apellidos")).trim();
            chat.setOnClickListener(v -> startActivity(ChatPasajeroTaxistaActivity.createIntent(this, nombre, null)
                    .putExtra(TaxiRepository.EXTRA_ID, serviceId)));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (map != null) map.onResume();
    }

    @Override
    protected void onPause() {
        if (map != null) map.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (map != null) map.onDetach();
        super.onDestroy();
    }
}
