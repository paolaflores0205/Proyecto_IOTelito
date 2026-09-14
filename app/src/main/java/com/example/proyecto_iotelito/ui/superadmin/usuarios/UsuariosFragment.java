package com.example.proyecto_iotelito.ui.superadmin.usuarios;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SuperadminSampleData;
import com.example.proyecto_iotelito.databinding.FragmentSuperadminUsuariosBinding;
import com.example.proyecto_iotelito.databinding.ItemSuperadminUserBinding;
import com.example.proyecto_iotelito.model.superadmin.AdminUser;

import java.util.Locale;

public class UsuariosFragment extends Fragment {
    private FragmentSuperadminUsuariosBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSuperadminUsuariosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.etUserSearch.addTextChangedListener(new SimpleTextWatcher(this::renderUsers));
        binding.chipGroupRoles.setOnCheckedStateChangeListener((group, checkedIds) -> renderUsers());
        binding.chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> renderUsers());
        renderUsers();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) renderUsers();
    }

    private void renderUsers() {
        String query = String.valueOf(binding.etUserSearch.getText()).trim().toLowerCase(Locale.ROOT);
        String role = selectedRole();
        Boolean active = selectedStatus();
        binding.userListContainer.removeAllViews();
        int visible = 0;

        for (AdminUser user : SuperadminSampleData.users()) {
            String searchable = (user.name + " " + user.email + " " + user.document).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            if (role != null && !role.equals(user.role)) continue;
            if (active != null && active != user.active) continue;

            ItemSuperadminUserBinding item = ItemSuperadminUserBinding.inflate(getLayoutInflater(), binding.userListContainer, false);
            item.tvUserName.setText(user.name);
            item.tvUserEmail.setText(user.email);
            item.tvUserRole.setText(getString(R.string.superadmin_role_format, user.role));
            item.tvUserRegistered.setText(getString(R.string.superadmin_registered_format, user.registeredAt));
            item.tvUserStatus.setText(user.active ? R.string.superadmin_filter_active : R.string.superadmin_filter_inactive);
            item.tvUserStatus.setTextColor(ContextCompat.getColor(requireContext(), user.active ? R.color.io_success_text : R.color.io_danger_text));
            item.btnUserDetail.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), DetalleUsuarioActivity.class);
                intent.putExtra(DetalleUsuarioActivity.EXTRA_USER_ID, user.id);
                startActivity(intent);
            });
            binding.userListContainer.addView(item.getRoot());
            visible++;
        }
        binding.tvUserCount.setText(getString(R.string.superadmin_showing_users, visible));
    }

    private String selectedRole() {
        int id = binding.chipGroupRoles.getCheckedChipId();
        if (id == R.id.chip_client) return "Cliente";
        if (id == R.id.chip_admin) return "Administrador";
        if (id == R.id.chip_driver) return "Taxista";
        if (id == R.id.chip_superadmin) return "Superadministrador";
        return null;
    }

    private Boolean selectedStatus() {
        int id = binding.chipGroupStatus.getCheckedChipId();
        if (id == R.id.chip_active) return true;
        if (id == R.id.chip_inactive) return false;
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable after;
        SimpleTextWatcher(Runnable after) { this.after = after; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override public void afterTextChanged(Editable s) { after.run(); }
    }
}
