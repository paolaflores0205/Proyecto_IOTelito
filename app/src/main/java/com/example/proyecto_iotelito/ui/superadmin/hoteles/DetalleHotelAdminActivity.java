package com.example.proyecto_iotelito.ui.superadmin.hoteles;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import androidx.lifecycle.ViewModelProvider;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminViewModel;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminDetalleHotelBinding;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class DetalleHotelAdminActivity extends AppCompatActivity {
    public static final String EXTRA_HOTEL_ID = "hotel_id";
    private ActivitySuperadminDetalleHotelBinding binding;
    private ManagedHotel hotel;
    private boolean changingSwitchProgrammatically;
    private SuperadminViewModel model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminDetalleHotelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnEdit.setEnabled(false);
        binding.btnChangeAdmin.setEnabled(false);
        binding.switchIot.setEnabled(false);
        model = new ViewModelProvider(this).get(SuperadminViewModel.class);
        model.hotel(getIntent().getIntExtra(EXTRA_HOTEL_ID, -1)).observe(this, row -> {
            hotel = row;
            if (row == null) return;
            binding.tvHotelName.setText(row.name);
            binding.tvAddress.setText(row.address);
            binding.tvAdmin.setText(row.administrator);
            binding.tvRooms.setText(row.rooms + "\n" + getString(R.string.superadmin_rooms));
            binding.photoGallery.show(row.photos, row.imageRes, null);
            binding.tvContactPhone.setText(row.phone.isEmpty() ? "Sin teléfono registrado" : row.phone);
            binding.tvContactEmail.setText(row.email.isEmpty() ? "Sin correo registrado" : row.email);
            setSwitchWithoutDialog(row.active);
            binding.btnEdit.setEnabled(true);
            binding.btnChangeAdmin.setEnabled(true);
            binding.switchIot.setEnabled(!Boolean.TRUE.equals(model.busy().getValue()));
        });
        model.busy().observe(this, busy -> binding.switchIot.setEnabled(hotel != null && !busy));
        model.result().observe(this, result -> {
            if (!result.consume()) return;
            if (hotel != null) setSwitchWithoutDialog(hotel.active);
            Snackbar.make(binding.getRoot(), result.error == null
                    ? getString(R.string.superadmin_hotel_status_updated) : result.error, Snackbar.LENGTH_LONG).show();
        });
        if (getIntent().getBooleanExtra("admin_alert", false)) {
            SuperadminRepository.getInstance(this).markAlertsRead(getIntent().getIntExtra(EXTRA_HOTEL_ID, -1));
        }

        binding.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(this, FormularioHotelActivity.class);
            intent.putExtra(FormularioHotelActivity.EXTRA_HOTEL_ID, hotel.id);
            startActivity(intent);
        });
        binding.btnChangeAdmin.setOnClickListener(v -> {
            Intent intent = new Intent(this, AsignarAdministradorActivity.class);
            intent.putExtra(AsignarAdministradorActivity.EXTRA_HOTEL_ID, hotel.id);
            startActivity(intent);
        });
        binding.switchIot.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!changingSwitchProgrammatically) confirmStatus(isChecked);
        });
    }

    private void confirmStatus(boolean requestedStatus) {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.superadmin_change_hotel_status)
                .setMessage(R.string.superadmin_change_hotel_status_message)
                .setNegativeButton(R.string.superadmin_cancel, (dialog, which) -> setSwitchWithoutDialog(!requestedStatus))
                .setOnCancelListener(dialog -> setSwitchWithoutDialog(!requestedStatus))
                .setPositiveButton(R.string.superadmin_confirm, (dialog, which) -> {
                    model.hotelStatus(hotel.id, requestedStatus);
                })
                .show();
    }

    private void setSwitchWithoutDialog(boolean checked) {
        changingSwitchProgrammatically = true;
        binding.switchIot.setChecked(checked);
        changingSwitchProgrammatically = false;
    }
}
