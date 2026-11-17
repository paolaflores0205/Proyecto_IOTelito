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
import androidx.lifecycle.ViewModelProvider;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminViewModel;
import com.example.proyecto_iotelito.databinding.FragmentSuperadminHotelesBinding;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;

import java.util.Locale;
import java.util.ArrayList;
import java.util.List;

public class HotelesAdminFragment extends Fragment {
    private FragmentSuperadminHotelesBinding binding;
    private HotelesAdapter adapter;
    private List<ManagedHotel> hotels = new ArrayList<>();
    private boolean loading = true;
    private String districtFilter;
    private String syncError;

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
        if (savedInstanceState != null) districtFilter = savedInstanceState.getString("district_filter");
        binding.actvDistrictFilter.setOnItemClickListener((parent, row, position, id) -> {
            districtFilter = position == 0 ? null : parent.getItemAtPosition(position).toString();
            renderHotels();
        });
        binding.chipUnassigned.setOnCheckedChangeListener((button, checked) -> renderHotels());
        if (savedInstanceState == null && getArguments() != null
                && "unassigned".equals(getArguments().getString("alert_filter"))) {
            districtFilter = null;
            binding.chipUnassigned.setChecked(true);
        }
        binding.fabAddHotel.setOnClickListener(v -> startActivity(new Intent(requireContext(), FormularioHotelActivity.class)));
        SuperadminViewModel model = new ViewModelProvider(this).get(SuperadminViewModel.class);
        model.hotelsState().observe(getViewLifecycleOwner(), state -> {
            hotels = state.hotels;
            updateDistricts();
            loading = state.loading; syncError = state.error;
            long active = state.hotels.stream().filter(hotel -> hotel.active).count();
            binding.tvHotelsSyncState.setText(state.loading ? "Cargando hoteles…"
                    : state.error == null ? active + " activos · " + (state.hotels.size() - active) + " inactivos" : state.error);
            updateSummary();
            renderHotels();
        });
        renderHotels();
    }

    private void updateSummary() {
        int unassigned = 0;
        for (ManagedHotel hotel : hotels) {
            if (!hotel.hasAdministrator()) unassigned++;
        }
        boolean waiting = hotels.isEmpty() && (loading || syncError != null);
        binding.tvHotelsTotal.setText(waiting ? "—" : String.valueOf(hotels.size()));
        binding.tvHotelsUnassigned.setText(waiting ? "—" : String.valueOf(unassigned));
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
        for (ManagedHotel hotel : hotels) {
            String searchable = (hotel.name + " " + hotel.address).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            if (district != null && !district.equals(hotel.district)) continue;
            if (binding.chipUnassigned.isChecked() && hotel.hasAdministrator()) continue;

            filtered.add(hotel);
        }
        adapter.setHotels(filtered);
        binding.cardHotelsEmpty.setVisibility(filtered.isEmpty() && !loading && syncError == null ? View.VISIBLE : View.GONE);
        binding.rvHotels.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void openDetail(int hotelId) {
        Intent intent = new Intent(requireContext(), DetalleHotelAdminActivity.class);
        intent.putExtra(DetalleHotelAdminActivity.EXTRA_HOTEL_ID, hotelId);
        startActivity(intent);
    }

    private String selectedDistrict() { return districtFilter; }

    private void updateDistricts() {
        java.util.Set<String> districts = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (ManagedHotel hotel : hotels) if (hotel.district != null && !hotel.district.trim().isEmpty()) districts.add(hotel.district.trim());
        if (districtFilter != null) districts.add(districtFilter);
        List<String> choices = new ArrayList<>();
        choices.add("Todos los distritos"); choices.addAll(districts);
        binding.actvDistrictFilter.setAdapter(new android.widget.ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, choices));
        binding.actvDistrictFilter.setText(districtFilter == null ? choices.get(0) : districtFilter, false);
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("district_filter", districtFilter);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
