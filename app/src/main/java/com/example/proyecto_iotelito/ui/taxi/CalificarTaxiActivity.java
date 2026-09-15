package com.example.proyecto_iotelito.ui.taxi;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.MainActivity;
import com.example.proyecto_iotelito.R;
import com.google.android.material.button.MaterialButton;

public class CalificarTaxiActivity extends AppCompatActivity {

    private int calificacion = 0;
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
            estrellas[i].setOnClickListener(v -> {
                calificacion = idx;
                actualizarEstrellas();
            });
        }

        MaterialButton btnEnviar = findViewById(R.id.btn_enviar_calificacion);
        btnEnviar.setOnClickListener(v -> irAlInicio());

        TextView tvVolver = findViewById(R.id.tv_volver_inicio);
        tvVolver.setOnClickListener(v -> irAlInicio());
    }

    private void actualizarEstrellas() {
        for (int i = 0; i < estrellas.length; i++) {
            estrellas[i].setImageResource(
                    i < calificacion ? R.drawable.ic_star : R.drawable.ic_star_border);
        }
    }

    private void irAlInicio() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("nav_destination", R.id.nav_reservas);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}
