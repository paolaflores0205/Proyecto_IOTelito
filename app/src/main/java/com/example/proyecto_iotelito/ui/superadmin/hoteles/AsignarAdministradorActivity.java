package com.example.proyecto_iotelito.ui.superadmin.hoteles;

import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.Toast;
import android.view.View;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import androidx.lifecycle.ViewModelProvider;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminViewModel;
import com.example.proyecto_iotelito.model.superadmin.AdminUser;
import com.google.android.material.radiobutton.MaterialRadioButton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminAsignarAdministradorBinding;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class AsignarAdministradorActivity extends AppCompatActivity {
    public static final String EXTRA_HOTEL_ID = "hotel_id";
    private ActivitySuperadminAsignarAdministradorBinding binding;
    private SuperadminViewModel model;
    private List<AdminUser> users = new ArrayList<>();
    private final Map<Integer, String> radioUserIds = new HashMap<>();
    private String selectedUserUid;
    private ManagedHotel hotel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminAsignarAdministradorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        if (savedInstanceState != null) selectedUserUid = savedInstanceState.getString("selected_admin_uid");
        model = new ViewModelProvider(this).get(SuperadminViewModel.class);
        model.hotel(getIntent().getIntExtra(EXTRA_HOTEL_ID, -1)).observe(this, row -> {
            hotel = row;
            if (row != null) {
                binding.tvHotelName.setText(row.name);
                binding.tvCurrentAdmin.setText(row.administrator);
            }
            updateButton();
            renderAdmins();
        });
        model.users().observe(this, rows -> { users = rows; renderAdmins(); });
        model.usersState().observe(this, state -> binding.tvAssignmentState.setText(state.loading
                ? "Cargando administradores…" : state.error == null
                ? "Solo administradores activos sin otro hotel asignado." : state.error));
        model.hotelsState().observe(this, state -> {
            if (state.error != null) binding.tvAssignmentState.setText(state.error);
        });
        model.busy().observe(this, busy -> updateButton());
        model.result().observe(this, result -> {
            if (!result.consume()) return;
            if (result.error != null) Snackbar.make(binding.getRoot(), result.error, Snackbar.LENGTH_LONG).show();
            else {
                Toast.makeText(this, R.string.superadmin_admin_assigned, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { renderAdmins(); }
        });
        binding.btnAssign.setOnClickListener(v -> confirmAssignment());
        binding.btnUnassign.setOnClickListener(v -> {
            if (hotel == null) return;
            final int id = hotel.id;
            new MaterialAlertDialogBuilder(this).setTitle("Desvincular administrador")
                    .setMessage("El hotel quedará sin administrador y la cuenta quedará disponible para otra asignación.")
                    .setNegativeButton(R.string.superadmin_cancel, null)
                    .setPositiveButton(R.string.superadmin_confirm, (dialog, which) -> model.assign(id, null)).show();
        });
        renderAdmins();
    }

    private void renderAdmins() {
        binding.radioGroupAdmins.setOnCheckedChangeListener(null);
        binding.radioGroupAdmins.removeAllViews();
        radioUserIds.clear();
        String query = binding.etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        boolean selectedVisible = false;
        for (AdminUser user : users) {
            if (!user.active || user.uid == null || !"Administrador".equals(user.role)) continue;
            if (user.hotelId != null && (hotel == null || user.hotelId != hotel.id)) continue;
            if (!(user.name + " " + user.email + " " + user.document).toLowerCase(Locale.ROOT).contains(query)) continue;
            MaterialRadioButton radio = new MaterialRadioButton(this);
            radio.setId(View.generateViewId());
            radio.setText(user.name + "\n" + user.email + " · " + user.document);
            int padding = (int) (12 * getResources().getDisplayMetrics().density);
            radio.setPadding(padding, padding, padding, padding);
            radioUserIds.put(radio.getId(), user.uid);
            binding.radioGroupAdmins.addView(radio);
            if (user.uid.equals(selectedUserUid)) {
                binding.radioGroupAdmins.check(radio.getId());
                selectedVisible = true;
            }
        }
        if (!selectedVisible && !users.isEmpty()) selectedUserUid = null;
        binding.radioGroupAdmins.setOnCheckedChangeListener((group, checkedId) -> {
            selectedUserUid = radioUserIds.get(checkedId);
            updateButton();
        });
        updateButton();
    }

    private void updateButton() {
        binding.btnAssign.setEnabled(hotel != null && selectedUserUid != null
                && !Boolean.TRUE.equals(model.busy().getValue()));
        binding.btnUnassign.setEnabled(hotel != null && hotel.hasAdministrator()
                && !Boolean.TRUE.equals(model.busy().getValue()));
    }

    private void confirmAssignment() {
        if (hotel == null || selectedUserUid == null) return;
        int checkedId = binding.radioGroupAdmins.getCheckedRadioButtonId();
        RadioButton selected = findViewById(checkedId);
        if (selected == null) return;
        String adminUid = selectedUserUid;
        int selectedHotelId = hotel.id;
        String selectedName = selected.getText().toString().split("\n")[0];
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.superadmin_assign_question)
                .setMessage(selectedName + "\n\n" + getString(R.string.superadmin_assign_message))
                .setNegativeButton(R.string.superadmin_cancel, null)
                .setPositiveButton(R.string.superadmin_confirm, (dialog, which) ->
                        model.assign(selectedHotelId, adminUid))
                .show();
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        out.putString("selected_admin_uid", selectedUserUid);
        super.onSaveInstanceState(out);
    }
}
