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
import com.example.proyecto_iotelito.databinding.FragmentSuperadminDashboardBinding;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminMainActivity;
import com.example.proyecto_iotelito.ui.superadmin.logs.RegistroLogsActivity;

public class DashboardFragment extends Fragment {
    private FragmentSuperadminDashboardBinding binding;

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
        binding.btnQuickUsers.setOnClickListener(v -> host.openTab(R.id.nav_superadmin_users));
        binding.btnQuickHotels.setOnClickListener(v -> host.openTab(R.id.nav_superadmin_hotels));
        binding.btnQuickReports.setOnClickListener(v -> host.openTab(R.id.nav_superadmin_reports));
        binding.btnQuickLogs.setOnClickListener(v -> startActivity(new Intent(requireContext(), RegistroLogsActivity.class)));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
