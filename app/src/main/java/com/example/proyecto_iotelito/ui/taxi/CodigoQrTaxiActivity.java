package com.example.proyecto_iotelito.ui.taxi;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;

public class CodigoQrTaxiActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_codigo_qr_taxi);

        View header = findViewById(R.id.sub_header);
        if (header != null) {
            ImageView ivBack = header.findViewById(R.id.iv_back);
            TextView tvTitle = header.findViewById(R.id.tv_title);
            if (tvTitle != null) tvTitle.setText(R.string.titulo_codigo_viaje);
            if (ivBack != null) ivBack.setOnClickListener(v -> finish());
        }

        TextView tvFinalizar = findViewById(R.id.tv_finalizar_viaje);
        tvFinalizar.setOnClickListener(v ->
                startActivity(new Intent(this, CalificarTaxiActivity.class)));
    }
}
