package com.example.proyecto_iotelito.ui.superadmin.hoteles;

import android.os.Bundle;
import android.widget.RadioButton;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SuperadminSampleData;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminAsignarAdministradorBinding;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class AsignarAdministradorActivity extends AppCompatActivity {
    public static final String EXTRA_HOTEL_ID = "hotel_id";
    private ActivitySuperadminAsignarAdministradorBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminAsignarAdministradorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        ManagedHotel hotel = SuperadminSampleData.hotel(getIntent().getIntExtra(EXTRA_HOTEL_ID, 1));
        binding.tvHotelName.setText(hotel.name);
        binding.tvCurrentAdmin.setText(hotel.administrator + "\njuan.huaman@iotelito.pe");
        binding.radioGroupAdmins.setOnCheckedChangeListener((group, checkedId) -> binding.btnAssign.setEnabled(checkedId != -1));
        binding.btnAssign.setOnClickListener(v -> confirmAssignment());
    }

    private void confirmAssignment() {
        int checkedId = binding.radioGroupAdmins.getCheckedRadioButtonId();
        RadioButton selected = findViewById(checkedId);
        String selectedName = selected.getText().toString().split("\n")[0];
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.superadmin_assign_question)
                .setMessage(selectedName + "\n\n" + getString(R.string.superadmin_assign_message))
                .setNegativeButton(R.string.superadmin_cancel, null)
                .setPositiveButton(R.string.superadmin_confirm, (dialog, which) ->
                        Snackbar.make(binding.getRoot(), R.string.superadmin_admin_assigned, Snackbar.LENGTH_LONG)
                                .setAction(R.string.btn_volver_inicio, v -> finish())
                                .show())
                .show();
    }
}
