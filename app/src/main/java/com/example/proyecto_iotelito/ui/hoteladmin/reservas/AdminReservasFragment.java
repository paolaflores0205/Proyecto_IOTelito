package com.example.proyecto_iotelito.ui.hoteladmin.reservas;

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
 * Pestaña "Reservas" del panel de Administrador de hotel. Alojará la lista
 * de reservas (Próximas / Hospedados / Checkout pendiente), el detalle de
 * cobro/checkout y el seguimiento de taxi. Placeholder navegable (andamiaje).
 */
public class AdminReservasFragment extends Fragment {

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
        binding.ivPlaceholderIcon.setImageResource(R.drawable.ic_calendar);
        binding.tvPlaceholderTitle.setText(R.string.hoteladmin_reservas_titulo);
        binding.tvPlaceholderMessage.setText(R.string.hoteladmin_reservas_placeholder);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
