package com.example.proyecto_iotelito.ui.superadmin.hoteles;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.ArrayAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SuperadminSampleData;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminFormularioHotelBinding;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.snackbar.Snackbar;

public class FormularioHotelActivity extends AppCompatActivity {
    public static final String EXTRA_HOTEL_ID = "hotel_id";
    private ActivitySuperadminFormularioHotelBinding binding;
    private FusedLocationProviderClient locationClient;
    private Double selectedLatitude;
    private Double selectedLongitude;

    private final ActivityResultLauncher<String[]> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean fine = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION));
                boolean coarse = Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_COARSE_LOCATION));
                if (fine || coarse) loadCurrentLocation();
                else Snackbar.make(binding.getRoot(), R.string.superadmin_location_permission_denied,
                        Snackbar.LENGTH_LONG).show();
            });

    private final ActivityResultLauncher<Intent> mapPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                Intent data = result.getData();
                if (result.getResultCode() == RESULT_OK && data != null) {
                    selectedLatitude = data.getDoubleExtra(
                            SeleccionarUbicacionActivity.EXTRA_LATITUDE, Double.NaN);
                    selectedLongitude = data.getDoubleExtra(
                            SeleccionarUbicacionActivity.EXTRA_LONGITUDE, Double.NaN);
                    if (Double.isNaN(selectedLatitude) || Double.isNaN(selectedLongitude)) {
                        selectedLatitude = null;
                        selectedLongitude = null;
                    }
                    updateLocationSummary();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminFormularioHotelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        locationClient = LocationServices.getFusedLocationProviderClient(this);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        String[] districts = {"Miraflores", "San Isidro", "Barranco", "Surco",
                "Centro Histórico", "Yanahuara", "Cercado", "Paracas"};
        String[] cities = {"Lima", "Arequipa", "Cusco", "Trujillo", "Paracas"};
        binding.actvDistrict.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, districts));
        binding.actvCity.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, cities));

        int hotelId = getIntent().getIntExtra(EXTRA_HOTEL_ID, -1);
        if (hotelId != -1) fillHotel(SuperadminSampleData.hotel(hotelId));
        binding.btnCurrentLocation.setOnClickListener(v -> requestCurrentLocation());
        binding.btnOpenMap.setOnClickListener(v -> openMapPicker());
        binding.btnSave.setOnClickListener(v -> save());
    }

    private void fillHotel(ManagedHotel hotel) {
        binding.toolbar.setTitle(R.string.superadmin_edit_hotel);
        binding.etName.setText(hotel.name);
        binding.etAddress.setText(hotel.address);
        binding.actvDistrict.setText(hotel.district, false);
        binding.actvCity.setText(SampleData.findById(hotel.id).city, false);
        binding.etPhone.setText("+51 1 445 6210");
        binding.etEmail.setText("contacto@hotelsol.pe");
        binding.etDescription.setText(
                "Hotel conectado con servicios inteligentes para una estadía cómoda y segura.");
        selectedLatitude = -12.1211;
        selectedLongitude = -77.0302;
        updateLocationSummary();
        binding.switchActive.setChecked(hotel.active);
    }

    private void requestCurrentLocation() {
        boolean fine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean coarse = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine || coarse) {
            loadCurrentLocation();
        } else {
            locationPermissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void loadCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) return;

        locationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location == null) {
                Snackbar.make(binding.getRoot(), R.string.superadmin_location_unavailable,
                        Snackbar.LENGTH_LONG).show();
                return;
            }
            selectedLatitude = location.getLatitude();
            selectedLongitude = location.getLongitude();
            updateLocationSummary();
        });
    }

    private void openMapPicker() {
        Intent intent = new Intent(this, SeleccionarUbicacionActivity.class);
        if (hasSelectedLocation()) {
            intent.putExtra(SeleccionarUbicacionActivity.EXTRA_LATITUDE, selectedLatitude);
            intent.putExtra(SeleccionarUbicacionActivity.EXTRA_LONGITUDE, selectedLongitude);
        }
        mapPickerLauncher.launch(intent);
    }

    private void updateLocationSummary() {
        if (hasSelectedLocation()) {
            binding.tvLocationStatus.setText(getString(R.string.superadmin_map_location_ready,
                    selectedLatitude, selectedLongitude));
            binding.tvLocationStatus.setTextColor(ContextCompat.getColor(this, R.color.io_teal));
        } else {
            binding.tvLocationStatus.setText(R.string.superadmin_map_location_pending);
            binding.tvLocationStatus.setTextColor(
                    ContextCompat.getColor(this, R.color.io_text_secondary));
        }
    }

    private boolean hasSelectedLocation() {
        return selectedLatitude != null && selectedLongitude != null
                && selectedLatitude >= -90 && selectedLatitude <= 90
                && selectedLongitude >= -180 && selectedLongitude <= 180;
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
        if (!hasSelectedLocation()) {
            binding.tvLocationStatus.setTextColor(ContextCompat.getColor(this,
                    com.google.android.material.R.color.design_default_color_error));
            Snackbar.make(binding.getRoot(), R.string.superadmin_invalid_coordinates,
                    Snackbar.LENGTH_LONG).show();
            valid = false;
        }
        if (!valid) return;
        Snackbar.make(binding.getRoot(), R.string.superadmin_hotel_saved, Snackbar.LENGTH_LONG)
                .setAction(R.string.btn_volver_inicio, v -> finish())
                .show();
    }
}
