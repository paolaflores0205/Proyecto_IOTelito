package com.example.proyecto_iotelito.ui.superadmin.hoteles;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SuperadminSampleData;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminDetalleHotelBinding;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class DetalleHotelAdminActivity extends AppCompatActivity {
    public static final String EXTRA_HOTEL_ID = "hotel_id";
    private ActivitySuperadminDetalleHotelBinding binding;
    private ManagedHotel hotel;
    private boolean changingSwitchProgrammatically;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminDetalleHotelBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        hotel = SuperadminSampleData.hotel(getIntent().getIntExtra(EXTRA_HOTEL_ID, 1));

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.tvHotelName.setText(hotel.name);
        binding.tvAddress.setText(hotel.address);
        binding.tvAdmin.setText(hotel.administrator);
        binding.tvRooms.setText(hotel.rooms + "\n" + getString(R.string.superadmin_rooms));
        binding.switchIot.setChecked(hotel.active);

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
                    hotel.active = requestedStatus;
                    Snackbar.make(binding.getRoot(), R.string.superadmin_hotel_status_updated, Snackbar.LENGTH_SHORT).show();
                })
                .show();
    }

    private void setSwitchWithoutDialog(boolean checked) {
        changingSwitchProgrammatically = true;
        binding.switchIot.setChecked(checked);
        changingSwitchProgrammatically = false;
    }
}
