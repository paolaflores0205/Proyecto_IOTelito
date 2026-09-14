package com.example.proyecto_iotelito.ui.superadmin.usuarios;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SuperadminSampleData;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminDetalleUsuarioBinding;
import com.example.proyecto_iotelito.model.superadmin.AdminUser;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

public class DetalleUsuarioActivity extends AppCompatActivity {
    public static final String EXTRA_USER_ID = "user_id";
    private ActivitySuperadminDetalleUsuarioBinding binding;
    private AdminUser user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminDetalleUsuarioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        user = SuperadminSampleData.user(getIntent().getIntExtra(EXTRA_USER_ID, 1));
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnToggleUser.setOnClickListener(v -> confirmStatusChange());
        render();
    }

    private void render() {
        String[] names = user.name.split(" ");
        String initials = names[0].substring(0, 1) + names[Math.min(1, names.length - 1)].substring(0, 1);
        binding.tvInitials.setText(initials);
        binding.tvName.setText(user.name);
        binding.tvStatus.setText(user.active ? R.string.superadmin_filter_active : R.string.superadmin_filter_inactive);
        binding.tvStatus.setTextColor(ContextCompat.getColor(this, user.active ? R.color.io_success_text : R.color.io_danger_text));
        binding.tvDocument.setText(getString(R.string.superadmin_document) + " " + user.document);
        binding.tvPhone.setText(getString(R.string.superadmin_phone) + " " + user.phone);
        binding.tvEmail.setText(getString(R.string.superadmin_email) + " " + user.email);
        binding.tvRole.setText(getString(R.string.superadmin_role) + " " + user.role);
        binding.tvRegistered.setText(getString(R.string.superadmin_registration_date) + " " + user.registeredAt);
        binding.tvLastAccess.setText(getString(R.string.superadmin_last_access) + " " + user.lastAccess);
        binding.btnToggleUser.setText(user.active ? R.string.superadmin_deactivate_user : R.string.superadmin_activate_user);
        int color = ContextCompat.getColor(this, user.active ? R.color.io_danger_text : R.color.io_teal);
        binding.btnToggleUser.setTextColor(color);
        binding.btnToggleUser.setStrokeColor(ColorStateList.valueOf(color));
    }

    private void confirmStatusChange() {
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(user.active ? R.string.superadmin_deactivate_question : R.string.superadmin_activate_question)
                .setMessage(getString(user.active ? R.string.superadmin_deactivate_message : R.string.superadmin_activate_message, user.name))
                .setNegativeButton(R.string.superadmin_cancel, null)
                .setPositiveButton(R.string.superadmin_confirm, (d, which) -> {
                    user.active = !user.active;
                    render();
                    Snackbar.make(binding.getRoot(), R.string.superadmin_user_status_updated, Snackbar.LENGTH_SHORT).show();
                });
        if (user.active) {
            EditText reason = new EditText(this);
            reason.setHint(R.string.superadmin_reason_hint);
            reason.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
            int padding = (int) (20 * getResources().getDisplayMetrics().density);
            reason.setPadding(padding, 0, padding, 0);
            dialog.setView(reason);
        }
        dialog.show();
    }
}
