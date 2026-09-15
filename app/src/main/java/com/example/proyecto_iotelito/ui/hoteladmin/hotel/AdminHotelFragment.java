package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

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
 * Pestaña "Hotel" del panel de Administrador de hotel. Alojará el dashboard
 * (resumen del día, alertas y accesos rápidos a datos del hotel, fotos,
 * habitaciones y servicios). Placeholder navegable (andamiaje).
 */
public class AdminHotelFragment extends Fragment {

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
        binding.ivPlaceholderIcon.setImageResource(R.drawable.ic_home);
        binding.tvPlaceholderTitle.setText(R.string.hoteladmin_hotel_titulo);
        binding.tvPlaceholderMessage.setText(R.string.hoteladmin_hotel_placeholder);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
