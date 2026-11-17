package com.example.proyecto_iotelito.ui.taxista;

import android.os.Bundle;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.ui.taxi.TaxiUi;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.*;

public class FinalizarServicioActivity extends AppCompatActivity {
    private TaxistaViewModel vm;
    private int id;
    private boolean sending;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        if (!TaxistaFlujo.sesion(this)) return;
        setContentView(R.layout.activity_finalizar_servicio);
        ((android.widget.TextView) findViewById(R.id.tv_title))
                .setText(R.string.taxi_api_final_title);
        vm = TaxistaFlujo.model(this);
        id =
                getIntent()
                        .getIntExtra(
                                com.example.proyecto_iotelito.data.TaxiRepository.EXTRA_ID, -1);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.taxi_scan_qr)
                .setOnClickListener(
                        v -> {
                            GmsBarcodeScannerOptions options =
                                    new GmsBarcodeScannerOptions.Builder()
                                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                                            .enableAutoZoom()
                                            .build();
                            GmsBarcodeScanning.getClient(this, options)
                                    .startScan()
                                    .addOnSuccessListener(
                                            code -> {
                                                if (code.getRawValue() != null)
                                                    finalizar(code.getRawValue());
                                            })
                                    .addOnFailureListener(
                                            e ->
                                                    TaxiUi.status(
                                                            this,
                                                            "No se pudo leer el QR. Reintenta o"
                                                                + " ingresa su código completo."));
                        });
        findViewById(R.id.btn_validar_codigo)
                .setOnClickListener(
                        v ->
                                finalizar(
                                        codigoEscrito(
                                                ((EditText) findViewById(R.id.et_codigo_manual))
                                                        .getText()
                                                        .toString())));
        vm.repo.error.observe(
                this,
                error -> {
                    if (error != null) {
                        sending = false;
                        enable(true);
                    }
                });
    }

    /** Acepta el código escrito con espacios o en mayúsculas: TX1- seguido de hexadecimal en minúsculas. */
    static String codigoEscrito(String texto) {
        String limpio = texto.replaceAll("\\s+", "");
        return limpio.regionMatches(true, 0, "TX1-", 0, 4)
                ? "TX1-" + limpio.substring(4).toLowerCase(java.util.Locale.ROOT)
                : limpio;
    }

    private void enable(boolean value) {
        findViewById(R.id.btn_validar_codigo).setEnabled(value);
        findViewById(R.id.taxi_scan_qr).setEnabled(value);
    }

    private void finalizar(String code) {
        if (sending) return;
        if (code.isEmpty()) {
            ((EditText) findViewById(R.id.et_codigo_manual)).setError("Ingresa el código del QR");
            return;
        }
        sending = true;
        enable(false);
        vm.repo.ejecutar(
                this,
                () -> vm.repo.finalizar(id, code),
                ok -> {
                    sending = false;
                    enable(true);
                    if (ok) {
                        TaxistaLocationService.stop(this);
                        startActivity(
                                TaxistaFlujo.intent(this, ServicioFinalizadoActivity.class, id));
                        finish();
                    }
                });
    }
}
