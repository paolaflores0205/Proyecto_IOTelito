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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SuperadminSampleData;
import com.example.proyecto_iotelito.databinding.FragmentSuperadminHotelesBinding;
import com.example.proyecto_iotelito.databinding.ItemSuperadminHotelBinding;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;

import java.util.Locale;

public class HotelesAdminFragment extends Fragment {
    private FragmentSuperadminHotelesBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentSuperadminHotelesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        binding.etHotelSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { renderHotels(); }
        });
        binding.chipGroupDistricts.setOnCheckedStateChangeListener((group, checkedIds) -> renderHotels());
        binding.fabAddHotel.setOnClickListener(v -> startActivity(new Intent(requireContext(), FormularioHotelActivity.class)));
        renderHotels();
    }

    private void renderHotels() {
        String query = String.valueOf(binding.etHotelSearch.getText()).trim().toLowerCase(Locale.ROOT);
        String district = selectedDistrict();
        binding.hotelListContainer.removeAllViews();
        for (ManagedHotel hotel : SuperadminSampleData.hotels()) {
            String searchable = (hotel.name + " " + hotel.address).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            if (district != null && !district.equals(hotel.district)) continue;

            ItemSuperadminHotelBinding item = ItemSuperadminHotelBinding.inflate(getLayoutInflater(), binding.hotelListContainer, false);
            item.ivHotelPhoto.setImageResource(hotel.imageRes);
            item.tvHotelName.setText(hotel.name);
            item.tvHotelAddress.setText(hotel.address);
            item.tvHotelAdmin.setText(getString(R.string.superadmin_admin_format, hotel.administrator));
            item.tvHotelRooms.setText(getString(R.string.superadmin_rooms_format, hotel.rooms));
            item.tvHotelReservations.setText(getString(R.string.superadmin_month_reservations_format, hotel.reservations));
            item.tvHotelStatus.setText(hotel.active ? R.string.superadmin_filter_active : R.string.superadmin_filter_inactive);
            item.tvHotelStatus.setTextColor(ContextCompat.getColor(requireContext(), hotel.active ? R.color.io_success_text : R.color.io_danger_text));
            View.OnClickListener open = v -> openDetail(hotel.id);
            item.btnHotelDetail.setOnClickListener(open);
            item.btnHotelManage.setOnClickListener(open);
            binding.hotelListContainer.addView(item.getRoot());
        }
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
