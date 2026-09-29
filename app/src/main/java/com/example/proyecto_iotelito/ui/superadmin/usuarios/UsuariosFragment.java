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
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SuperadminSampleData;
import com.example.proyecto_iotelito.databinding.FragmentSuperadminUsuariosBinding;
import com.example.proyecto_iotelito.model.superadmin.AdminUser;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class UsuariosFragment extends Fragment {
    private FragmentSuperadminUsuariosBinding binding;
    private UsuariosAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSuperadminUsuariosBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        updateSummary();
        adapter = new UsuariosAdapter(this::openUserDetail);
        binding.rvUsers.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvUsers.setAdapter(adapter);
        binding.etUserSearch.addTextChangedListener(new SimpleTextWatcher(this::renderUsers));
        binding.chipGroupRoles.setOnCheckedStateChangeListener((group, checkedIds) -> renderUsers());
        binding.chipGroupStatus.setOnCheckedStateChangeListener((group, checkedIds) -> renderUsers());
        renderUsers();
    }

    private void updateSummary() {
        int total = SuperadminSampleData.users().size();
        int active = 0;
        for (AdminUser user : SuperadminSampleData.users()) {
            if (user.active) active++;
        }
        binding.tvUsersTotal.setText(String.valueOf(total));
        binding.tvUsersStatusSummary.setText(
                getString(R.string.superadmin_users_status_summary, active, total - active));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            updateSummary();
            renderUsers();
        }
    }

    private void renderUsers() {
        String query = String.valueOf(binding.etUserSearch.getText()).trim().toLowerCase(Locale.ROOT);
        String role = selectedRole();
        Boolean active = selectedStatus();
        List<AdminUser> filtered = new ArrayList<>();

        for (AdminUser user : SuperadminSampleData.users()) {
            String searchable = (user.name + " " + user.email + " " + user.document).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            if (role != null && !role.equals(user.role)) continue;
            if (active != null && active != user.active) continue;

            filtered.add(user);
        }
        adapter.setUsers(filtered);
        binding.tvUserCount.setText(getString(R.string.superadmin_showing_users, filtered.size()));
        binding.cardUsersEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvUsers.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openUserDetail(AdminUser user) {
        Intent intent = new Intent(requireContext(), DetalleUsuarioActivity.class);
        intent.putExtra(DetalleUsuarioActivity.EXTRA_USER_ID, user.id);
        startActivity(intent);
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
