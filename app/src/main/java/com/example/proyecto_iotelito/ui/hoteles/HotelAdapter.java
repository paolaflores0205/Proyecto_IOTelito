package com.example.proyecto_iotelito.ui.hoteles;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelMedia;
import com.example.proyecto_iotelito.model.Hotel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter reutilizable para los listados estáticos de hoteles del cliente.
 * Permite mostrar las tarjetas horizontales de Explorar y las tarjetas
 * verticales de Resultados/Ver todos sin duplicar la lógica de enlace.
 */
public class HotelAdapter extends RecyclerView.Adapter<HotelAdapter.HotelViewHolder> {

    public enum TipoTarjeta { HORIZONTAL, VERTICAL }

    public interface OnHotelClickListener {
        void onHotelClick(Hotel hotel);
    }

    private final List<Hotel> hoteles = new ArrayList<>();
    private final TipoTarjeta tipoTarjeta;
    private final OnHotelClickListener listener;

    public HotelAdapter(TipoTarjeta tipoTarjeta, OnHotelClickListener listener) {
        this.tipoTarjeta = tipoTarjeta;
        this.listener = listener;
    }

    public void submitList(List<Hotel> nuevosHoteles) {
        hoteles.clear();
        hoteles.addAll(nuevosHoteles);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HotelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = tipoTarjeta == TipoTarjeta.HORIZONTAL
                ? R.layout.item_hotel_card_horizontal
                : R.layout.item_hotel_card_vertical;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new HotelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HotelViewHolder holder, int position) {
        holder.bind(hoteles.get(position), tipoTarjeta, listener);

        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
        int espacio = dp(holder.itemView, 12);
        if (tipoTarjeta == TipoTarjeta.HORIZONTAL) {
            params.setMarginEnd(position == hoteles.size() - 1 ? 0 : espacio);
            params.topMargin = 0;
        } else {
            params.topMargin = position == 0 ? 0 : dp(holder.itemView, 16);
            params.setMarginEnd(0);
        }
        holder.itemView.setLayoutParams(params);
    }

    @Override
    public int getItemCount() {
        return hoteles.size();
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    static class HotelViewHolder extends RecyclerView.ViewHolder {
        private final ImageView foto;
        private final TextView ubicacion;
        private final TextView rating;
        private final TextView nombre;
        private final TextView precio;
        private final View botonDetalle;

        HotelViewHolder(@NonNull View itemView) {
            super(itemView);
            foto = itemView.findViewById(R.id.iv_photo);
            TextView city = itemView.findViewById(R.id.tv_city);
            TextView location = itemView.findViewById(R.id.tv_location);
            ubicacion = city != null ? city : location;
            rating = itemView.findViewById(R.id.tv_rating);
            nombre = itemView.findViewById(R.id.tv_name);
            precio = itemView.findViewById(R.id.tv_price);
            botonDetalle = itemView.findViewById(R.id.btn_ver_detalle);
        }

        void bind(Hotel hotel, TipoTarjeta tipo, OnHotelClickListener listener) {
            foto.setImageResource(HotelMedia.hotelImage(hotel.id));
            ubicacion.setText(tipo == TipoTarjeta.HORIZONTAL ? hotel.city : hotel.address);
            rating.setText(String.format(Locale.getDefault(), "%.1f", hotel.rating));
            nombre.setText(hotel.name);

            String precioFormateado = String.format(Locale.getDefault(), "S/ %.0f", hotel.pricePerNight);
            precio.setText(tipo == TipoTarjeta.HORIZONTAL
                    ? itemView.getContext().getString(R.string.precio_por_noche, precioFormateado)
                    : itemView.getContext().getString(R.string.desde_precio, precioFormateado));

            View.OnClickListener click = v -> listener.onHotelClick(hotel);
            itemView.setOnClickListener(click);
            if (botonDetalle != null) {
                botonDetalle.setOnClickListener(click);
            }
        }
    }
}
