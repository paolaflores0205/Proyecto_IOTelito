package com.example.proyecto_iotelito.ui.superadmin.reportes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.FragmentSuperadminReportesBinding;

public class ReportesFragment extends Fragment {
    private FragmentSuperadminReportesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSuperadminReportesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        String[] hotels = {getString(R.string.superadmin_all_hotels), "Hotel Sol de Miraflores", "Barranco Art Boutique"};
        binding.actvHotelSelector.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, hotels));
        binding.actvHotelSelector.setText(hotels[0], false);
        binding.chipGroupPeriod.setOnCheckedStateChangeListener((group, checkedIds) -> updateRange());
        updateRange();
    }

    private void updateRange() {
        int id = binding.chipGroupPeriod.getCheckedChipId();
        String range;
        if (id == R.id.chip_daily) {
            range = "13 de setiembre de 2026";
            binding.weeklyBookingsChart.setData(
                    new String[]{"08h", "10h", "12h", "14h", "16h", "18h"},
                    new int[]{4, 7, 12, 9, 15, 11});
        } else if (id == R.id.chip_yearly) {
            range = "Enero - Diciembre, 2026";
            binding.weeklyBookingsChart.setData(
                    new String[]{"Ene", "Mar", "May", "Jul", "Set", "Nov"},
                    new int[]{420, 510, 635, 720, 856, 910});
        } else {
            range = "01 de setiembre - 30 de setiembre, 2026";
            binding.weeklyBookingsChart.setData(
                    new String[]{"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"},
                    new int[]{18, 26, 34, 42, 38, 51, 45});
        }
        binding.tvSelectedRange.setText(getString(R.string.superadmin_selected_range, range));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
