package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.data.hoteladmin.HotelAdminLocalStore;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminDatosHotelBinding;

/** Consulta y edición de los datos generales del hotel (adm-datos-hotel). */
public class DatosHotelActivity extends AppCompatActivity {

    private ActivityHoteladminDatosHotelBinding binding;
    private boolean formLoaded;
    private FirebaseHotelAdminRepository remote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!com.example.proyecto_iotelito.ui.hoteladmin.HotelAdminAccessGuard.allow(this)) return;
        binding = ActivityHoteladminDatosHotelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_datos_hotel);
        binding.toolbar.ivBack.setOnClickListener(v -> finish());
        binding.btnFotos.setOnClickListener(v ->
                startActivity(new Intent(this, FotosHotelActivity.class)));
        binding.btnGuardar.setOnClickListener(v -> guardar());
        binding.btnGuardar.setEnabled(false);
        if (FirebaseHotelAdminRepository.remote(this)) {
            remote = new FirebaseHotelAdminRepository(this);
            remote.observeHotel().observe(this, state -> {
                binding.btnGuardar.setEnabled(state.data != null);
                if (state.error != null) Toast.makeText(this, state.error, Toast.LENGTH_LONG).show();
                if (state.data != null && !formLoaded) {
                    binding.etNombre.setText(state.data.name);
                    binding.etDireccion.setText(state.data.address);
                    binding.etDescripcion.setText(state.data.description);
                    binding.etAtracciones.setText(state.data.attractions);
                    binding.etTelefono.setText(state.data.phone);
                    binding.etEmail.setText(state.data.email);
                    formLoaded = true;
                }
            });
            return;
        }
        HotelAdminLocalStore.get(this).load(error -> {
            if (isFinishing() || isDestroyed()) return;
            if (error != null) {
                Toast.makeText(this, R.string.hoteladmin_error_carga_local, Toast.LENGTH_LONG).show();
                return;
            }
            SuperadminRepository.getInstance(this).hotel(HotelAdminSampleData.HOTEL_ID)
                    .observe(this, hotel -> {
                        if (hotel == null) return;
                        HotelAdminSampleData.aplicarHotelCompartido(hotel);
                        if (!formLoaded) {
                            cargarDatos();
                            formLoaded = true;
                            binding.btnGuardar.setEnabled(true);
                        }
                    });
        });
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

        SuperadminRepository.HotelDetails details = new SuperadminRepository.HotelDetails(
                nombre, direccion, valor(binding.etDescripcion.getText()),
                valor(binding.etAtracciones.getText()), valor(binding.etTelefono.getText()), email);
        binding.btnGuardar.setEnabled(false);
        if (remote != null) {
            remote.saveDetails(details, error -> {
                if (isFinishing() || isDestroyed()) return;
                binding.btnGuardar.setEnabled(true);
                if (error != null) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); return; }
                Toast.makeText(this, R.string.hoteladmin_datos_guardados, Toast.LENGTH_SHORT).show();
                finish();
            });
            return;
        }
        SuperadminRepository.getInstance(this).updateHotelDetailsFromAdmin(
                HotelAdminSampleData.HOTEL_ID, details, (id, error) -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (error != null) {
                        binding.btnGuardar.setEnabled(true);
                        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
                        return;
                    }
                    HotelAdminSampleData.hotelNombre = details.name;
                    HotelAdminSampleData.hotelDireccion = details.address;
                    HotelAdminSampleData.hotelDescripcion = details.description;
                    HotelAdminSampleData.hotelAtracciones = details.attractions;
                    HotelAdminSampleData.hotelTelefono = details.phone;
                    HotelAdminSampleData.hotelEmail = details.email;
                    Toast.makeText(this, R.string.hoteladmin_datos_guardados, Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private String valor(CharSequence text) {
        return text == null ? "" : text.toString().trim();
    }
}
