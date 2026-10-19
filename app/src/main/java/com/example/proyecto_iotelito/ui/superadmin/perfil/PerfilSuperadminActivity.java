package com.example.proyecto_iotelito.ui.superadmin.perfil;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminPerfilBinding;
import com.example.proyecto_iotelito.ui.auth.LoginActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class PerfilSuperadminActivity extends AppCompatActivity {
    private ActivitySuperadminPerfilBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminPerfilBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.btnLogout.setOnClickListener(v -> confirmLogout());
        binding.btnAdminAlerts.setOnClickListener(v -> startActivity(new Intent(this,
                com.example.proyecto_iotelito.ui.superadmin.notificaciones.AvisosSuperadminActivity.class)));
    }

    private void confirmLogout() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.superadmin_profile_logout_question)
                .setMessage(R.string.superadmin_profile_logout_message)
                .setNegativeButton(R.string.superadmin_cancel, null)
                .setPositiveButton(R.string.superadmin_profile_logout, (dialog, which) -> {
                    com.example.proyecto_iotelito.data.SessionRepository.cerrar(this);
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                })
                .show();
    }
}
