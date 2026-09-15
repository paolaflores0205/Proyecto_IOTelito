package com.example.proyecto_iotelito.ui.hoteladmin.reportes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.FragmentPlaceholderBinding;

/**
 * Pestaña "Reportes" del panel de Administrador de hotel. Alojará los
 * indicadores del hotel y el reporte de ingresos por servicios adicionales
 * (ordenados de menor a mayor). Placeholder navegable (andamiaje).
 */
public class AdminReportesFragment extends Fragment {

    private FragmentPlaceholderBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPlaceholderBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.ivPlaceholderIcon.setImageResource(R.drawable.ic_bar_chart);
        binding.tvPlaceholderTitle.setText(R.string.hoteladmin_reportes_titulo);
        binding.tvPlaceholderMessage.setText(R.string.hoteladmin_reportes_placeholder);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
