package com.example.proyecto_iotelito.ui.taxi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.proyecto_iotelito.MainActivity;
import com.example.proyecto_iotelito.R;
import com.google.android.material.button.MaterialButton;

public class CalificarTaxiActivity extends TaxiServiceScreen {

    private int calificacion = 0;
    private boolean sending;
    private final ImageView[] estrellas = new ImageView[5];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calificar_taxi);

        estrellas[0] = findViewById(R.id.iv_taxi_star_1);
        estrellas[1] = findViewById(R.id.iv_taxi_star_2);
        estrellas[2] = findViewById(R.id.iv_taxi_star_3);
        estrellas[3] = findViewById(R.id.iv_taxi_star_4);
        estrellas[4] = findViewById(R.id.iv_taxi_star_5);

        for (int i = 0; i < estrellas.length; i++) {
            final int idx = i + 1;
            estrellas[i].setOnClickListener(
                    v -> {
                        calificacion = idx;
                        actualizarEstrellas();
                    });
        }

        MaterialButton btnEnviar = findViewById(R.id.btn_enviar_calificacion);
        btnEnviar.setEnabled(false);
        btnEnviar.setOnClickListener(
                v -> {
                    if (calificacion < 1) {
                        TaxiUi.status(this, "Selecciona entre 1 y 5 estrellas.");
                        return;
                    }
                    if (sending) return;
                    final String comment =
                            ((android.widget.EditText) findViewById(R.id.et_comentarios_taxi))
                                    .getText()
                                    .toString()
                                    .trim();
                    sending = true;
                    btnEnviar.setEnabled(false);
                    com.example.proyecto_iotelito.data.taxiapi.TaxiTasks.run(
                            this,
                            () -> api.calificarServicio(serviceId, calificacion, comment),
                            saved -> irAlInicio(),
                            error -> {
                                sending = false;
                                btnEnviar.setEnabled(true);
                                TaxiUi.status(this, error);
                            });
                });

        TextView tvVolver = findViewById(R.id.tv_volver_inicio);
        tvVolver.setOnClickListener(v -> irAlInicio());
        observeService();
    }

    protected void render(org.json.JSONObject s) {
        org.json.JSONObject driver = s.optJSONObject("taxista");
        if (driver != null) {
            String name = driver.optString("nombres") + " " + driver.optString("apellidos");
            ((TextView) findViewById(R.id.taxi_rating_name)).setText(name);
            ((TextView) findViewById(R.id.taxi_rating_initials))
                    .setText(name.isEmpty() ? "" : name.substring(0, 1));
            org.json.JSONObject car = driver.optJSONObject("vehiculo");
            ((TextView) findViewById(R.id.taxi_rating_vehicle))
                    .setText(
                            car.optString("marca")
                                    + " "
                                    + car.optString("modelo")
                                    + " · "
                                    + car.optString("placa"));
        }
        boolean finalizado = "FINALIZADO".equals(s.optString("estado"));
        boolean rated = !s.isNull("calificacion");
        findViewById(R.id.btn_enviar_calificacion).setEnabled(finalizado && !rated && !sending);
        if (rated) {
            calificacion = s.optJSONObject("calificacion").optInt("puntuacion");
            actualizarEstrellas();
            TaxiUi.status(this, "Ya calificaste este servicio.");
        } else if (!finalizado)
            TaxiUi.status(this, "Podrás calificar cuando el conductor finalice el viaje.");
    }

    private void actualizarEstrellas() {
        for (int i = 0; i < estrellas.length; i++) {
            estrellas[i].setImageResource(
                    i < calificacion ? R.drawable.ic_star : R.drawable.ic_star_border);
        }
    }

    private void irAlInicio() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, R.id.nav_reservas);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
