package com.example.proyecto_iotelito.ui.superadmin.hoteles;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminSeleccionarUbicacionBinding;

import java.util.Locale;

public class SeleccionarUbicacionActivity extends AppCompatActivity {
    public static final String EXTRA_LATITUDE = "selected_latitude";
    public static final String EXTRA_LONGITUDE = "selected_longitude";
    private static final double DEFAULT_LATITUDE = -12.0464;
    private static final double DEFAULT_LONGITUDE = -77.0428;

    private ActivitySuperadminSeleccionarUbicacionBinding binding;
    private double selectedLatitude = DEFAULT_LATITUDE;
    private double selectedLongitude = DEFAULT_LONGITUDE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminSeleccionarUbicacionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        selectedLatitude = getIntent().getDoubleExtra(EXTRA_LATITUDE, DEFAULT_LATITUDE);
        selectedLongitude = getIntent().getDoubleExtra(EXTRA_LONGITUDE, DEFAULT_LONGITUDE);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnConfirmLocation.setOnClickListener(v -> confirmLocation());
        configureMap(binding.mapWebView);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void configureMap(WebView webView) {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new MapBridge(), "Android");
        String url = String.format(Locale.US,
                "file:///android_asset/location_picker.html?lat=%f&lng=%f",
                selectedLatitude, selectedLongitude);
        webView.loadUrl(url);
    }

    private void confirmLocation() {
        Intent result = new Intent();
        result.putExtra(EXTRA_LATITUDE, selectedLatitude);
        result.putExtra(EXTRA_LONGITUDE, selectedLongitude);
        setResult(RESULT_OK, result);
        finish();
    }

    private final class MapBridge {
        @JavascriptInterface
        public void onMapMoved(double latitude, double longitude) {
            selectedLatitude = latitude;
            selectedLongitude = longitude;
            runOnUiThread(() -> binding.tvSelectedCoordinates.setText(
                    getString(R.string.superadmin_location_picker_point,
                            selectedLatitude, selectedLongitude)));
        }
    }

    @Override
    protected void onDestroy() {
        binding.mapWebView.removeJavascriptInterface("Android");
        binding.mapWebView.destroy();
        super.onDestroy();
    }
}
