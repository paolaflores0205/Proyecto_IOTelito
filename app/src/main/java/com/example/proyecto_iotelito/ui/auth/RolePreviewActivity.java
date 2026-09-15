package com.example.proyecto_iotelito.ui.auth;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ActivityRolePreviewBinding;
import com.example.proyecto_iotelito.model.auth.UserRole;

public class RolePreviewActivity extends AppCompatActivity {

    private static final String EXTRA_ROLE = "extra_role";

    public static Intent createIntent(Context context, UserRole role) {
        return new Intent(context, RolePreviewActivity.class)
                .putExtra(EXTRA_ROLE, role.name());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityRolePreviewBinding binding = ActivityRolePreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String roleName = getIntent().getStringExtra(EXTRA_ROLE);
        UserRole role;
        try {
            role = UserRole.valueOf(roleName == null ? "" : roleName);
        } catch (IllegalArgumentException exception) {
            finish();
            return;
        }

        if (role == UserRole.HOTEL_ADMIN) {
            binding.toolbar.setTitle(R.string.role_admin_name);
            binding.roleInitials.setText(R.string.role_admin_initials);
            binding.roleName.setText(R.string.role_admin_name);
            binding.roleFeatures.setText(R.string.role_admin_features);
        } else if (role == UserRole.TAXISTA) {
            binding.toolbar.setTitle(R.string.role_driver_name);
            binding.roleInitials.setText(R.string.role_driver_initials);
            binding.roleName.setText(R.string.role_driver_name);
            binding.roleFeatures.setText(R.string.role_driver_features);
        } else {
            finish();
            return;
        }

        binding.toolbar.setNavigationOnClickListener(view -> finish());
        binding.changeAccountButton.setOnClickListener(view -> finish());
    }
}
