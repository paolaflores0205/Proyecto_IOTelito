package com.example.proyecto_iotelito.ui.superadmin.dashboard;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import androidx.lifecycle.ViewModelProvider;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminViewModel;
import java.util.ArrayList;
import java.util.List;
import com.example.proyecto_iotelito.databinding.FragmentSuperadminDashboardBinding;
import com.example.proyecto_iotelito.model.superadmin.AdminUser;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminMainActivity;
import com.example.proyecto_iotelito.ui.superadmin.logs.RegistroLogsActivity;
import com.example.proyecto_iotelito.ui.superadmin.perfil.PerfilSuperadminActivity;

public class DashboardFragment extends Fragment {
    private FragmentSuperadminDashboardBinding binding;
    private List<AdminUser> users = new ArrayList<>();
    private List<ManagedHotel> hotels = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSuperadminDashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        SuperadminMainActivity host = (SuperadminMainActivity) requireActivity();
        renderSummary();
        binding.btnQuickUsers.setOnClickListener(v -> host.openTab(R.id.nav_superadmin_users));
        binding.btnQuickHotels.setOnClickListener(v -> host.openTab(R.id.nav_superadmin_hotels));
        binding.btnQuickReports.setOnClickListener(v -> host.openTab(R.id.nav_superadmin_reports));
        binding.btnQuickLogs.setOnClickListener(v -> startActivity(new Intent(requireContext(), RegistroLogsActivity.class)));
        binding.btnSuperadminProfile.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), PerfilSuperadminActivity.class)));
        binding.cardInactiveAlert.setOnClickListener(v -> host.openTab(R.id.nav_superadmin_users));
        binding.cardUnassignedAlert.setOnClickListener(v -> host.openTab(R.id.nav_superadmin_hotels));
        SuperadminViewModel model = new ViewModelProvider(this).get(SuperadminViewModel.class);
        model.users().observe(getViewLifecycleOwner(), rows -> { users = rows; renderSummary(); });
        model.hotels().observe(getViewLifecycleOwner(), rows -> { hotels = rows; renderSummary(); });
        model.logs().observe(getViewLifecycleOwner(), rows -> {
            android.widget.TextView[] recent = {binding.tvRecentOne, binding.tvRecentTwo, binding.tvRecentThree};
            for (int i = 0; i < recent.length; i++) {
                recent[i].setVisibility(i < rows.size() ? View.VISIBLE : View.GONE);
                if (i < rows.size()) {
                    com.example.proyecto_iotelito.model.superadmin.ActivityLog log = rows.get(i);
                    recent[i].setText(log.title + "\n" + log.detail + "\n" + log.time);
                }
            }
        });
    }

    private void renderSummary() {
        int activeUsers = 0;
        int inactiveUsers = 0;
        int administrators = 0;
        for (AdminUser user : users) {
            if (user.active) activeUsers++; else inactiveUsers++;
            if ("Administrador".equals(user.role)) administrators++;
        }

        int unassignedHotels = 0;
        int bookings = 0;
        for (ManagedHotel hotel : hotels) {
            bookings += hotel.reservations;
            if (hotel.administratorId == null) unassignedHotels++;
        }

        binding.tvDashboardUsers.setText(String.valueOf(activeUsers));
        binding.tvDashboardHotels.setText(String.valueOf(hotels.size()));
        binding.tvDashboardAdmins.setText(String.valueOf(administrators));
        binding.tvDashboardBookings.setText(String.valueOf(bookings));
        binding.tvInactiveAlert.setText(getResources().getQuantityString(
                R.plurals.superadmin_inactive_users_dynamic, inactiveUsers, inactiveUsers));
        binding.tvUnassignedAlert.setText(getResources().getQuantityString(
                R.plurals.superadmin_unassigned_hotels_dynamic, unassignedHotels, unassignedHotels));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
