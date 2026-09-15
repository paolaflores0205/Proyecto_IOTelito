package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminDatosHotelBinding;

/** Consulta y edición de los datos generales del hotel (adm-datos-hotel). */
public class DatosHotelActivity extends AppCompatActivity {

    private ActivityHoteladminDatosHotelBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHoteladminDatosHotelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_datos_hotel);
        binding.toolbar.ivBack.setOnClickListener(v -> finish());
        binding.btnFotos.setOnClickListener(v ->
                startActivity(new Intent(this, FotosHotelActivity.class)));
        binding.btnGuardar.setOnClickListener(v -> guardar());
        cargarDatos();
    }

    private void cargarDatos() {
        binding.etNombre.setText(HotelAdminSampleData.hotelNombre);
        binding.etDireccion.setText(HotelAdminSampleData.hotelDireccion);
        binding.etDescripcion.setText(HotelAdminSampleData.hotelDescripcion);
        binding.etAtracciones.setText(HotelAdminSampleData.hotelAtracciones);
        binding.etTelefono.setText(HotelAdminSampleData.hotelTelefono);
        binding.etEmail.setText(HotelAdminSampleData.hotelEmail);
    }

    private void guardar() {
        String nombre = valor(binding.etNombre.getText());
        String direccion = valor(binding.etDireccion.getText());
        String email = valor(binding.etEmail.getText());
        boolean valido = true;

        if (TextUtils.isEmpty(nombre)) {
            binding.etNombre.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (TextUtils.isEmpty(direccion)) {
            binding.etDireccion.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (TextUtils.isEmpty(email)) {
            binding.etEmail.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError(getString(R.string.hoteladmin_email_invalido));
            valido = false;
        }
        if (!valido) return;

        HotelAdminSampleData.hotelNombre = nombre;
        HotelAdminSampleData.hotelDireccion = direccion;
        HotelAdminSampleData.hotelDescripcion = valor(binding.etDescripcion.getText());
        HotelAdminSampleData.hotelAtracciones = valor(binding.etAtracciones.getText());
        HotelAdminSampleData.hotelTelefono = valor(binding.etTelefono.getText());
        HotelAdminSampleData.hotelEmail = email;
        Toast.makeText(this, R.string.hoteladmin_datos_guardados, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String valor(CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }
}
