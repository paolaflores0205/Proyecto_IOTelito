package com.example.proyecto_iotelito.ui.hoteladmin.reservas;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminEstadoTaxiBinding;
import com.example.proyecto_iotelito.model.hoteladmin.ServicioTaxi;

/** Pantalla adm-estado-taxi con los cinco estados definidos por el proyecto. */
public class EstadoTaxiActivity extends AppCompatActivity {
    private ActivityHoteladminEstadoTaxiBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHoteladminEstadoTaxiBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.tvTitle.setText(R.string.hoteladmin_taxi_titulo);
        binding.toolbar.ivBack.setOnClickListener(v -> finish());
        mostrarTaxi();
    }

    private void mostrarTaxi() {
        ServicioTaxi taxi = HotelAdminSampleData.taxiActivo();
        binding.tvHuesped.setText(taxi.huespedNombre);
        binding.tvHabitacion.setText(taxi.habitacion);
        binding.tvDestino.setText(taxi.destino);
        binding.tvConductor.setText(taxi.conductor);
        binding.tvVehiculo.setText(taxi.vehiculo);
        binding.tvPlaca.setText(taxi.placa);
        binding.tvTelefono.setText(taxi.telefono);
        binding.tvRating.setText(getString(R.string.hoteladmin_taxi_rating, taxi.rating));
        TextView[] estados = {binding.tvSolicitado, binding.tvAsignado, binding.tvEnCamino,
                binding.tvEnTraslado, binding.tvFinalizado};
        int actual = taxi.estado.ordinal();
        for (int i = 0; i < estados.length; i++) {
            boolean alcanzado = i <= actual;
            estados[i].setTextColor(ContextCompat.getColor(this,
                    alcanzado ? R.color.io_teal : R.color.io_text_muted));
            estados[i].setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                    i == actual ? R.color.io_info_bg : R.color.io_surface)));
            estados[i].setTypeface(null, i == actual ? android.graphics.Typeface.BOLD
                    : android.graphics.Typeface.NORMAL);
        }
        binding.tvEstadoActual.setText(getString(R.string.hoteladmin_taxi_estado_actual,
                estados[actual].getText()));
        binding.btnContactar.setOnClickListener(v -> llamar(taxi.huespedTelefono));
        binding.btnLlamarConductor.setOnClickListener(v -> llamar(taxi.telefono));
        binding.btnVerReserva.setOnClickListener(v -> {
            Intent intent = new Intent(this, CobroCheckoutActivity.class);
            intent.putExtra(CobroCheckoutActivity.EXTRA_RESERVA_ID, 101);
            startActivity(intent);
        });
    }

    /** Abre el marcador del teléfono (huésped o conductor). */
    private void llamar(String telefono) {
        startActivity(new Intent(Intent.ACTION_DIAL,
                Uri.parse("tel:" + telefono.replace(" ", ""))));
    }
}
