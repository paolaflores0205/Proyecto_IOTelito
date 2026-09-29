package com.example.proyecto_iotelito.ui.superadmin.hoteles;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ItemSuperadminHotelBinding;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;

import java.util.ArrayList;
import java.util.List;

public class HotelesAdapter extends RecyclerView.Adapter<HotelesAdapter.HotelViewHolder> {
    public interface OnHotelClickListener { void onHotelClick(ManagedHotel hotel); }

    private final List<ManagedHotel> hotels = new ArrayList<>();
    private final OnHotelClickListener listener;

    public HotelesAdapter(OnHotelClickListener listener) {
        this.listener = listener;
    }

    public void setHotels(List<ManagedHotel> newHotels) {
        hotels.clear();
        hotels.addAll(newHotels);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HotelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSuperadminHotelBinding binding = ItemSuperadminHotelBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new HotelViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull HotelViewHolder holder, int position) {
        holder.bind(hotels.get(position));
    }

    @Override
    public int getItemCount() { return hotels.size(); }

    class HotelViewHolder extends RecyclerView.ViewHolder {
        private final ItemSuperadminHotelBinding binding;

        HotelViewHolder(ItemSuperadminHotelBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(ManagedHotel hotel) {
            binding.ivHotelPhoto.setImageResource(hotel.imageRes);
            binding.tvHotelName.setText(hotel.name);
            binding.tvHotelAddress.setText(hotel.address);
            binding.tvHotelAdmin.setText(itemView.getContext().getString(
                    R.string.superadmin_admin_format, hotel.administrator));
            binding.tvHotelRooms.setText(itemView.getContext().getString(
                    R.string.superadmin_rooms_format, hotel.rooms));
            binding.tvHotelReservations.setText(itemView.getContext().getString(
                    R.string.superadmin_month_reservations_format, hotel.reservations));
            binding.tvHotelStatus.setText(hotel.active
                    ? R.string.superadmin_filter_active : R.string.superadmin_filter_inactive);
            binding.tvHotelStatus.setTextColor(ContextCompat.getColor(itemView.getContext(),
                    hotel.active ? R.color.io_success_text : R.color.io_danger_text));
            binding.btnHotelManage.setOnClickListener(v -> listener.onHotelClick(hotel));
            binding.getRoot().setOnClickListener(v -> listener.onHotelClick(hotel));
        }
    }
}
