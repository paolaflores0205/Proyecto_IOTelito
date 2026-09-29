package com.example.proyecto_iotelito.ui.superadmin.hoteles;

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
import com.example.proyecto_iotelito.databinding.FragmentSuperadminHotelesBinding;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class HotelesAdminFragment extends Fragment {
    private FragmentSuperadminHotelesBinding binding;
    private HotelesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSuperadminHotelesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        updateSummary();
        adapter = new HotelesAdapter(hotel -> openDetail(hotel.id));
        binding.rvHotels.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvHotels.setAdapter(adapter);
        binding.etHotelSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { renderHotels(); }
        });
        binding.chipGroupDistricts.setOnCheckedStateChangeListener((group, checkedIds) -> renderHotels());
        binding.fabAddHotel.setOnClickListener(v -> startActivity(new Intent(requireContext(), FormularioHotelActivity.class)));
        renderHotels();
    }

    private void updateSummary() {
        int unassigned = 0;
        for (ManagedHotel hotel : SuperadminSampleData.hotels()) {
            if ("Sin asignar".equalsIgnoreCase(hotel.administrator)) unassigned++;
        }
        binding.tvHotelsTotal.setText(String.valueOf(SuperadminSampleData.hotels().size()));
        binding.tvHotelsUnassigned.setText(String.valueOf(unassigned));
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) {
            updateSummary();
            renderHotels();
        }
    }

    private void renderHotels() {
        String query = String.valueOf(binding.etHotelSearch.getText()).trim().toLowerCase(Locale.ROOT);
        String district = selectedDistrict();
        List<ManagedHotel> filtered = new ArrayList<>();
        for (ManagedHotel hotel : SuperadminSampleData.hotels()) {
            String searchable = (hotel.name + " " + hotel.address).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            if (district != null && !district.equals(hotel.district)) continue;

            filtered.add(hotel);
        }
        adapter.setHotels(filtered);
        binding.cardHotelsEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        binding.rvHotels.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openDetail(int hotelId) {
        Intent intent = new Intent(requireContext(), DetalleHotelAdminActivity.class);
        intent.putExtra(DetalleHotelAdminActivity.EXTRA_HOTEL_ID, hotelId);
        startActivity(intent);
    }

    private String selectedDistrict() {
        int id = binding.chipGroupDistricts.getCheckedChipId();
        if (id == R.id.chip_miraflores) return "Miraflores";
        if (id == R.id.chip_san_isidro) return "San Isidro";
        if (id == R.id.chip_barranco) return "Barranco";
        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
