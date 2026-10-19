package com.example.proyecto_iotelito.ui.perfil;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.NotificacionCliente;

import java.util.ArrayList;
import java.util.List;

public class NotificacionAdapter extends RecyclerView.Adapter<NotificacionAdapter.ViewHolder> {

    public interface Listener {
        void onClick(NotificacionCliente notificacion);
    }

    private final List<NotificacionCliente> items = new ArrayList<>();
    private final Listener listener;

    public NotificacionAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submitList(List<NotificacionCliente> notificaciones) {
        items.clear();
        items.addAll(notificaciones);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notificacion_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private final View root;
        private final ImageView icono;
        private final TextView titulo;
        private final TextView tiempo;
        private final View punto;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.card_notificacion_root);
            icono = itemView.findViewById(R.id.iv_icono_notif);
            titulo = itemView.findViewById(R.id.tv_titulo_notif);
            tiempo = itemView.findViewById(R.id.tv_tiempo_notif);
            punto = itemView.findViewById(R.id.view_dot_sin_leer);
        }

        void bind(NotificacionCliente notificacion) {
            root.setBackgroundResource(notificacion.leida
                    ? R.drawable.bg_notif_read_card : R.drawable.bg_notif_unread_card);
            icono.setImageResource(notificacion.tipo == NotificacionCliente.Tipo.PAGO
                    ? R.drawable.ic_credit_card : R.drawable.ic_calendar);
            titulo.setText(notificacion.titulo);
            tiempo.setText(DateUtils.getRelativeTimeSpanString(
                    notificacion.creadaEn,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS));
            punto.setVisibility(notificacion.leida ? View.GONE : View.VISIBLE);
            root.setOnClickListener(v -> listener.onClick(notificacion));
        }
    }
}
