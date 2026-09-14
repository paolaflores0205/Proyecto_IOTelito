package com.example.proyecto_iotelito.ui.superadmin.hoteles;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SuperadminSampleData;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminFormularioHotelBinding;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.google.android.material.snackbar.Snackbar;

public class FormularioHotelActivity extends AppCompatActivity {
    public static final String EXTRA_HOTEL_ID = "hotel_id";
    private ActivitySuperadminFormularioHotelBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminFormularioHotelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        String[] districts = {"Miraflores", "San Isidro", "Barranco", "Surco"};
        String[] cities = {"Lima", "Arequipa", "Cusco"};
        binding.actvDistrict.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, districts));
        binding.actvCity.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities));

        int hotelId = getIntent().getIntExtra(EXTRA_HOTEL_ID, -1);
        if (hotelId != -1) fillHotel(SuperadminSampleData.hotel(hotelId));
        binding.btnSave.setOnClickListener(v -> save());
    }

    private void fillHotel(ManagedHotel hotel) {
        binding.toolbar.setTitle(R.string.superadmin_edit_hotel);
        binding.etName.setText(hotel.name);
        binding.etAddress.setText(hotel.address);
        binding.actvDistrict.setText(hotel.district, false);
        binding.actvCity.setText("Lima", false);
        binding.etPhone.setText("+51 1 445 6210");
        binding.etEmail.setText("contacto@hotelsol.pe");
        binding.etDescription.setText("Hotel conectado con servicios inteligentes para una estadía cómoda y segura.");
        binding.switchActive.setChecked(hotel.active);
    }

    private void save() {
        binding.tilName.setError(null);
        binding.tilAddress.setError(null);
        String name = String.valueOf(binding.etName.getText()).trim();
        String address = String.valueOf(binding.etAddress.getText()).trim();
        String email = String.valueOf(binding.etEmail.getText()).trim();
        boolean valid = true;
        if (name.isEmpty()) {
            binding.tilName.setError(getString(R.string.superadmin_required_field));
            valid = false;
        }
        if (address.isEmpty()) {
            binding.tilAddress.setError(getString(R.string.superadmin_required_field));
            valid = false;
        }
        if (!email.isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.setError("Correo no válido");
            valid = false;
        }
        if (!valid) return;
        Snackbar.make(binding.getRoot(), R.string.superadmin_hotel_saved, Snackbar.LENGTH_LONG)
                .setAction(R.string.btn_volver_inicio, v -> finish())
                .show();
    }
}
