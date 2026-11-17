package com.example.proyecto_iotelito.ui.taxi;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.taxiapi.TaxiTasks;
import com.google.zxing.*;
import com.google.zxing.common.BitMatrix;

import org.json.JSONObject;

public class CodigoQrTaxiActivity extends TaxiServiceScreen {
    private boolean loading, shown;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_codigo_qr_taxi);
        ((TextView) findViewById(R.id.tv_title)).setText(R.string.titulo_codigo_viaje);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.tv_finalizar_viaje)
                .setOnClickListener(v -> startActivity(intent(CalificarTaxiActivity.class)));
        observeService();
    }

    protected void render(JSONObject s) {
        boolean done = "FINALIZADO".equals(s.optString("estado"));
        findViewById(R.id.tv_finalizar_viaje).setVisibility(done ? View.VISIBLE : View.GONE);
        JSONObject d = s.optJSONObject("taxista");
        ((TextView) findViewById(R.id.taxi_qr_details))
                .setText(
                        s.optString("estado").replace('_', ' ')
                                + (d == null
                                        ? ""
                                        : "\n"
                                                + d.optString("nombres")
                                                + " · "
                                                + d.optJSONObject("vehiculo").optString("placa"))
                                + "\n"
                                + s.optJSONObject("aeropuerto").optString("nombre"));
        if (done) {
            ((ImageView) findViewById(R.id.taxi_qr_image)).setImageDrawable(null);
            shown = false;
            return;
        }
        if (!loading && !shown) {
            loading = true;
            TaxiTasks.run(
                    this,
                    () -> {
                        String code = api.verQr(serviceId).optString("codigo");
                        BitMatrix bits =
                                new MultiFormatWriter()
                                        .encode(code, BarcodeFormat.QR_CODE, 640, 640);
                        int[] pixels = new int[640 * 640];
                        for (int y = 0; y < 640; y++)
                            for (int x = 0; x < 640; x++)
                                pixels[y * 640 + x] = bits.get(x, y) ? 0xff000000 : 0xffffffff;
                        return Bitmap.createBitmap(pixels, 640, 640, Bitmap.Config.ARGB_8888);
                    },
                    bitmap -> {
                        loading = false;
                        if (!getLifecycle()
                                .getCurrentState()
                                .isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) return;
                        shown = true;
                        ((ImageView) findViewById(R.id.taxi_qr_image)).setImageBitmap(bitmap);
                    },
                    error -> {
                        loading = false;
                        TaxiUi.status(this, error);
                    });
        }
    }

    @Override
    protected void onPause() {
        ((ImageView) findViewById(R.id.taxi_qr_image)).setImageDrawable(null);
        shown = false;
        super.onPause();
    }
}
