package com.example.proyecto_iotelito.ui.booking;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelMedia;
import com.example.proyecto_iotelito.model.OpcionHabitaciones;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;

import java.util.List;
import java.util.Locale;

public class OpcionHabitacionesAdapter extends RecyclerView.Adapter<OpcionHabitacionesAdapter.ViewHolder> {

    public interface OnOpcionClickListener {
        void onOpcionClick(OpcionHabitaciones opcion);
    }

    private final int hotelId;
    private final int noches;
    private final boolean modoReserva;
    private final List<OpcionHabitaciones> opciones;
    private final OnOpcionClickListener listener;

    public OpcionHabitacionesAdapter(int hotelId, int noches, boolean modoReserva,
                                     List<OpcionHabitaciones> opciones,
                                     OnOpcionClickListener listener) {
        this.hotelId = hotelId;
        this.noches = noches;
        this.modoReserva = modoReserva;
        this.opciones = opciones;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_opcion_habitaciones, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(opciones.get(position));
    }

    @Override
    public int getItemCount() {
        return opciones.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView foto;
        private final TextView titulo;
        private final TextView habitaciones;
        private final TextView capacidad;
        private final TextView precioNoche;
        private final TextView precioTotal;
        private final View boton;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            foto = itemView.findViewById(R.id.iv_opcion_foto);
            titulo = itemView.findViewById(R.id.tv_opcion_titulo);
            habitaciones = itemView.findViewById(R.id.tv_opcion_habitaciones);
            capacidad = itemView.findViewById(R.id.tv_opcion_capacidad);
            precioNoche = itemView.findViewById(R.id.tv_opcion_precio_noche);
            precioTotal = itemView.findViewById(R.id.tv_opcion_precio_total);
            boton = itemView.findViewById(R.id.btn_ver_opcion);
        }

        void bind(OpcionHabitaciones opcion) {
            Habitacion primeraHabitacion = opcion.habitaciones().get(0);
            foto.setImageResource(primeraHabitacion.fotoRes != 0
                    ? primeraHabitacion.fotoRes
                    : HotelMedia.galleryImage(hotelId, getBindingAdapterPosition() + 1));
            titulo.setText(opcion.titulo());
            if (modoReserva) {
                habitaciones.setText(opcion.resumenTipos());
                capacidad.setText(itemView.getResources().getQuantityString(
                        R.plurals.opcion_capacidad_habitaciones,
                        opcion.cantidadHabitaciones(),
                        opcion.aforo(), opcion.cantidadHabitaciones()));
            } else {
                habitaciones.setText(primeraHabitacion.nombreCodigo);
                capacidad.setText(itemView.getContext().getString(
                        R.string.capacidad_area_habitacion,
                        opcion.aforo(), primeraHabitacion.areaM2));
            }
            precioNoche.setText(itemView.getContext().getString(
                    R.string.opcion_precio_noche,
                    String.format(Locale.US, "%,.0f", opcion.precioPorNoche())));
            if (modoReserva) {
                precioTotal.setVisibility(View.VISIBLE);
                precioTotal.setText(itemView.getResources().getQuantityString(
                        R.plurals.opcion_precio_estadia,
                        noches,
                        String.format(Locale.US, "%,.0f", opcion.precioPorNoche() * noches), noches));
            } else {
                precioTotal.setVisibility(View.GONE);
                ((TextView) boton).setText(R.string.ver_caracteristicas_habitacion);
            }

            View.OnClickListener click = v -> listener.onOpcionClick(opcion);
            boton.setOnClickListener(click);
            itemView.setOnClickListener(click);
        }
    }
}
