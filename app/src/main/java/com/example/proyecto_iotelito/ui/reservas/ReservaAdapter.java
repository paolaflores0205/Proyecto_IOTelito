package com.example.proyecto_iotelito.ui.reservas;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** RecyclerView con dos tipos de fila: encabezado de sección y reserva. */
public class ReservaAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TIPO_ENCABEZADO = 0;
    private static final int TIPO_RESERVA = 1;

    public interface OnReservaClickListener {
        void onReservaClick(Reserva reserva);
    }

    public static final class Item {
        final Integer tituloRes;
        final Reserva reserva;

        private Item(Integer tituloRes, Reserva reserva) {
            this.tituloRes = tituloRes;
            this.reserva = reserva;
        }

        public static Item encabezado(int tituloRes) {
            return new Item(tituloRes, null);
        }

        public static Item reserva(Reserva reserva) {
            return new Item(null, reserva);
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final OnReservaClickListener listener;

    public ReservaAdapter(OnReservaClickListener listener) {
        this.listener = listener;
    }

    public void submitList(List<Item> nuevosItems) {
        items.clear();
        items.addAll(nuevosItems);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).reserva == null ? TIPO_ENCABEZADO : TIPO_RESERVA;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TIPO_ENCABEZADO) {
            return new EncabezadoViewHolder(inflater.inflate(R.layout.item_reserva_section_header, parent, false));
        }
        return new ReservaViewHolder(inflater.inflate(R.layout.item_reserva_card, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Item item = items.get(position);
        if (holder instanceof EncabezadoViewHolder) {
            ((EncabezadoViewHolder) holder).titulo.setText(item.tituloRes);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            params.topMargin = position == 0 ? 0 : dp(holder.itemView, 20);
            holder.itemView.setLayoutParams(params);
        } else {
            ((ReservaViewHolder) holder).bind(item.reserva, listener);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.itemView.getLayoutParams();
            boolean siguienteEsReserva = position + 1 < items.size()
                    && items.get(position + 1).reserva != null;
            params.bottomMargin = siguienteEsReserva ? dp(holder.itemView, 12) : 0;
            holder.itemView.setLayoutParams(params);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static int dp(View view, int value) {
        return Math.round(value * view.getResources().getDisplayMetrics().density);
    }

    static class EncabezadoViewHolder extends RecyclerView.ViewHolder {
        final TextView titulo;

        EncabezadoViewHolder(@NonNull View itemView) {
            super(itemView);
            titulo = itemView.findViewById(R.id.tv_reserva_section_title);
        }
    }

    static class ReservaViewHolder extends RecyclerView.ViewHolder {
        final TextView hotelNombre;
        final TextView habitacion;
        final TextView fechasHuespedes;
        final TextView precioTotal;
        final TextView estado;
        final View botonDetalle;

        ReservaViewHolder(@NonNull View itemView) {
            super(itemView);
            hotelNombre = itemView.findViewById(R.id.tv_hotel_nombre);
            habitacion = itemView.findViewById(R.id.tv_habitacion);
            fechasHuespedes = itemView.findViewById(R.id.tv_fechas_huespedes);
            precioTotal = itemView.findViewById(R.id.tv_precio_total);
            estado = itemView.findViewById(R.id.tv_estado);
            botonDetalle = itemView.findViewById(R.id.btn_ver_detalle_reserva);
        }

        void bind(Reserva reserva, OnReservaClickListener listener) {
            Hotel hotel = SampleData.findById(reserva.hotelId);
            hotelNombre.setText(hotel.name);
            habitacion.setText(reserva.roomName);
            fechasHuespedes.setText(reserva.rangoFechasTexto() + " · " + reserva.huespedes);
            precioTotal.setText("S/ " + String.format(Locale.US, "%,.0f", reserva.precioTotal));

            boolean enCurso = reserva.esActiva() && reserva.estaEnCurso();
            int colorFondo = enCurso ? R.color.io_teal : EstadoUi.colorFondo(reserva.estado);
            int colorTexto = enCurso ? R.color.white : EstadoUi.colorTexto(reserva.estado);
            estado.setText(enCurso
                    ? itemView.getContext().getString(R.string.estado_en_curso)
                    : EstadoUi.texto(itemView.getContext(), reserva.estado));
            estado.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(itemView.getContext(), colorFondo)));
            estado.setTextColor(ContextCompat.getColor(itemView.getContext(), colorTexto));

            View.OnClickListener click = v -> listener.onReservaClick(reserva);
            itemView.setOnClickListener(click);
            botonDetalle.setOnClickListener(click);
        }
    }
}
