package com.example.proyecto_iotelito.ui.superadmin.hoteles;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.*;
import android.webkit.*;
import android.view.inputmethod.EditorInfo;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminSeleccionarUbicacionBinding;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import java.util.*;

public class SeleccionarUbicacionActivity extends AppCompatActivity {
    public static final String EXTRA_LATITUDE = "selected_latitude", EXTRA_LONGITUDE = "selected_longitude";
    public static final String EXTRA_ADDRESS = "selected_address", EXTRA_DISTRICT = "selected_district",
            EXTRA_CITY = "selected_city", EXTRA_QUERY = "address_query";
    private ActivitySuperadminSeleccionarUbicacionBinding binding;
    private double latitude, longitude;
    private String address = "", district = "", city = "";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int generation;
    private boolean destroyed;
    private boolean pageLoaded;
    private Double pendingLatitude, pendingLongitude;
    private Geocoder geocoder;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivitySuperadminSeleccionarUbicacionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        latitude = state == null ? getIntent().getDoubleExtra(EXTRA_LATITUDE, -12.0464) : state.getDouble("lat");
        longitude = state == null ? getIntent().getDoubleExtra(EXTRA_LONGITUDE, -77.0428) : state.getDouble("lng");
        geocoder = new Geocoder(this, new Locale("es", "PE"));
        binding.etMapSearch.setText(getIntent().getStringExtra(EXTRA_QUERY));
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnConfirmLocation.setEnabled(false);
        binding.btnConfirmLocation.setOnClickListener(v -> {
            if (address.isEmpty()) return;
            Intent data = new Intent().putExtra(EXTRA_LATITUDE, latitude).putExtra(EXTRA_LONGITUDE, longitude)
                    .putExtra(EXTRA_ADDRESS, address).putExtra(EXTRA_DISTRICT, district).putExtra(EXTRA_CITY, city);
            setResult(RESULT_OK, data); finish();
        });
        binding.btnSearchAddress.setOnClickListener(v -> search());
        binding.etMapSearch.setOnEditorActionListener((v, action, event) -> {
            if (action == EditorInfo.IME_ACTION_SEARCH) { search(); return true; }
            return false;
        });
        configureMap();
    }
    private void search() {
        String query = String.valueOf(binding.etMapSearch.getText()).trim();
        if (query.isEmpty()) { binding.etMapSearch.setError("Escribe una dirección con distrito y ciudad."); return; }
        int token = ++generation;
        handler.removeCallbacksAndMessages(null);
        address = "";
        binding.btnConfirmLocation.setEnabled(false);
        binding.tvSelectedCoordinates.setText("Buscando dirección…");
        if (!Geocoder.isPresent()) { fail(token, "El dispositivo no tiene servicio de direcciones."); return; }
        geocoder.getFromLocationName(query, 5, new Geocoder.GeocodeListener() {
            @Override public void onGeocode(List<Address> results) { runOnUiThread(() -> {
                if (destroyed || token != generation) return;
                if (results.isEmpty()) { fail(token, "No se encontró. Añade distrito, ciudad y país."); return; }
                String[] names = new String[results.size()];
                for (int i = 0; i < names.length; i++) names[i] = results.get(i).getAddressLine(0);
                new MaterialAlertDialogBuilder(SeleccionarUbicacionActivity.this)
                    .setTitle("Elige la dirección y revisa el punto")
                    .setItems(names, (dialog, which) -> {
                        if (token != generation) return;
                        Address found = results.get(which);
                        moveMap(found.getLatitude(), found.getLongitude());
                    }).setNegativeButton("Cancelar", (dialog, which) -> resolve(latitude, longitude)).show();
            }); }
            @Override public void onError(String message) { fail(token, "No se pudo buscar. Comprueba tu conexión e intenta otra vez."); }
        });
    }
    private void moveMap(double lat, double lng) {
        latitude = lat; longitude = lng;
        if (!pageLoaded) { pendingLatitude = lat; pendingLongitude = lng; return; }
        binding.mapWebView.evaluateJavascript(String.format(Locale.US, "window.selectPoint(%f,%f)", lat, lng), null);
    }
    private void resolve(double lat, double lng) {
        latitude = lat; longitude = lng;
        int token = ++generation;
        address = ""; district = ""; city = "";
        binding.btnConfirmLocation.setEnabled(false);
        binding.tvSelectedCoordinates.setText("Obteniendo dirección del punto…");
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> {
            if (destroyed || token != generation) return;
            if (!Geocoder.isPresent()) { fail(token, "Servicio de direcciones no disponible. Intenta en otro dispositivo."); return; }
            geocoder.getFromLocation(lat, lng, 1, new Geocoder.GeocodeListener() {
                @Override public void onGeocode(List<Address> rows) { runOnUiThread(() -> {
                    if (destroyed || token != generation) return;
                    if (rows.isEmpty() || rows.get(0).getAddressLine(0) == null) {
                        fail(token, "No se encontró dirección. Acerca el punto a una calle o busca una dirección."); return;
                    }
                    Address found = rows.get(0);
                    address = found.getAddressLine(0);
                    district = first(found.getSubLocality(), found.getLocality());
                    city = first(found.getSubAdminArea(), found.getLocality());
                    binding.tvSelectedCoordinates.setText(address + "\nDistrito: " + (district.isEmpty() ? "Completar en formulario" : district)
                            + " · Ciudad: " + (city.isEmpty() ? "Completar en formulario" : city));
                    binding.btnConfirmLocation.setEnabled(true);
                }); }
                @Override public void onError(String message) { fail(token, "No se pudo obtener la dirección. Comprueba internet y vuelve a mover el mapa."); }
            });
        }, 600);
    }
    private static String first(String a, String b) { return a != null && !a.isEmpty() ? a : b == null ? "" : b; }
    private void fail(int token, String message) { runOnUiThread(() -> {
        if (!destroyed && token == generation) { binding.tvSelectedCoordinates.setText(message); binding.btnConfirmLocation.setEnabled(false); }
    }); }
    @SuppressLint("SetJavaScriptEnabled") private void configureMap() {
        WebView map = binding.mapWebView;
        map.getSettings().setJavaScriptEnabled(true);
        map.getSettings().setDomStorageEnabled(true);
        map.addJavascriptInterface(new MapBridge(), "Android");
        map.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) { return true; }
            @Override public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                if (pendingLatitude != null) { moveMap(pendingLatitude, pendingLongitude); pendingLatitude = null; }
                else resolve(latitude, longitude);
            }
        });
        map.loadUrl(String.format(Locale.US, "file:///android_asset/location_picker.html?lat=%f&lng=%f", latitude, longitude));
    }
    private final class MapBridge {
        @JavascriptInterface public void onMapMoved(double lat, double lng) {
            if (!Double.isFinite(lat) || !Double.isFinite(lng) || Math.abs(lat) > 90 || Math.abs(lng) > 180) return;
            runOnUiThread(() -> { if (!destroyed) resolve(lat, lng); });
        }
    }
    @Override protected void onSaveInstanceState(Bundle out) { out.putDouble("lat", latitude); out.putDouble("lng", longitude); super.onSaveInstanceState(out); }
    @Override protected void onDestroy() {
        destroyed = true; generation++; handler.removeCallbacksAndMessages(null);
        binding.mapWebView.removeJavascriptInterface("Android"); binding.mapWebView.destroy(); super.onDestroy();
    }
}
