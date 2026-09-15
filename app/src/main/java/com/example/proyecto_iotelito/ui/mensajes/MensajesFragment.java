package com.example.proyecto_iotelito.ui.mensajes;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Conversacion;
import com.example.proyecto_iotelito.ui.reservas.ChatHotelActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pestaña "Mensajes". Muestra la lista de conversaciones entre el cliente
 * y sus distintas estadías / hoteles (Mockup de Figma).
 */
public class MensajesFragment extends Fragment {

    private EditText etBuscar;
    private TextView chipTodos;
    private TextView chipHospedados;
    private TextView chipPorLlegar;
    private LinearLayout conversacionesContainer;
    private TextView tvVacio;

    private Conversacion.Categoria categoriaActiva = Conversacion.Categoria.TODOS;
    private String textoBusqueda = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mensajes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etBuscar = view.findViewById(R.id.et_buscar_conversaciones);
        chipTodos = view.findViewById(R.id.chip_todos);
        chipHospedados = view.findViewById(R.id.chip_hospedados);
        chipPorLlegar = view.findViewById(R.id.chip_por_llegar);
        conversacionesContainer = view.findViewById(R.id.conversaciones_container);
        tvVacio = view.findViewById(R.id.tv_mensajes_vacio);

        configurarChipsFiltro();
        configurarBuscador();

        renderizarConversaciones();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresca la lista al regresar del chat (para ocultar badges de mensajes leídos)
        renderizarConversaciones();
    }

    private void configurarChipsFiltro() {
        chipTodos.setOnClickListener(v -> cambiarCategoria(Conversacion.Categoria.TODOS));
        chipHospedados.setOnClickListener(v -> cambiarCategoria(Conversacion.Categoria.HOSPEDADOS));
        chipPorLlegar.setOnClickListener(v -> cambiarCategoria(Conversacion.Categoria.POR_LLEGAR));
    }

    private void cambiarCategoria(Conversacion.Categoria categoria) {
        if (this.categoriaActiva == categoria) {
            return;
        }
        this.categoriaActiva = categoria;
        actualizarEstiloChips();
        renderizarConversaciones();
    }

    private void actualizarEstiloChips() {
        aplicarEstiloChip(chipTodos, categoriaActiva == Conversacion.Categoria.TODOS);
        aplicarEstiloChip(chipHospedados, categoriaActiva == Conversacion.Categoria.HOSPEDADOS);
        aplicarEstiloChip(chipPorLlegar, categoriaActiva == Conversacion.Categoria.POR_LLEGAR);
    }

    private void aplicarEstiloChip(TextView chip, boolean seleccionado) {
        if (seleccionado) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.io_teal));
            chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.BOLD);
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.io_text_secondary));
            chip.setTypeface(chip.getTypeface(), android.graphics.Typeface.NORMAL);
        }
    }

    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                textoBusqueda = s != null ? s.toString().trim().toLowerCase(Locale.getDefault()) : "";
                renderizarConversaciones();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    private void renderizarConversaciones() {
        conversacionesContainer.removeAllViews();

        List<Conversacion> filtradas = new ArrayList<>();
        for (Conversacion conv : SampleData.CONVERSACIONES) {
            // Filtro de categoría
            if (categoriaActiva != Conversacion.Categoria.TODOS && conv.categoria != categoriaActiva) {
                continue;
            }
            // Filtro de texto
            if (!textoBusqueda.isEmpty()) {
                String nombre = conv.nombreContacto.toLowerCase(Locale.getDefault());
                String hab = conv.habitacion.toLowerCase(Locale.getDefault());
                String msg = conv.ultimoMensaje.toLowerCase(Locale.getDefault());
                if (!nombre.contains(textoBusqueda) && !hab.contains(textoBusqueda) && !msg.contains(textoBusqueda)) {
                    continue;
                }
            }
            filtradas.add(conv);
        }

        if (filtradas.isEmpty()) {
            tvVacio.setVisibility(View.VISIBLE);
        } else {
            tvVacio.setVisibility(View.GONE);
            LayoutInflater inflater = LayoutInflater.from(requireContext());
            for (Conversacion conv : filtradas) {
                View item = inflater.inflate(R.layout.item_conversacion, conversacionesContainer, false);
                bindConversacion(item, conv);
                conversacionesContainer.addView(item);
            }
        }
    }

    private void bindConversacion(View item, Conversacion conv) {
        ((TextView) item.findViewById(R.id.tv_avatar_inicial)).setText(conv.avatarInicial);
        ((TextView) item.findViewById(R.id.tv_nombre_contacto)).setText(conv.nombreContacto);
        ((TextView) item.findViewById(R.id.tv_habitacion)).setText(conv.habitacion);
        ((TextView) item.findViewById(R.id.tv_ultimo_mensaje)).setText(conv.ultimoMensaje);
        ((TextView) item.findViewById(R.id.tv_hora)).setText(conv.hora);

        TextView tvBadge = item.findViewById(R.id.tv_badge_sin_leer);
        if (conv.mensajesSinLeer > 0) {
            tvBadge.setVisibility(View.VISIBLE);
            tvBadge.setText(String.valueOf(conv.mensajesSinLeer));
        } else {
            tvBadge.setVisibility(View.GONE);
        }

        View row = item.findViewById(R.id.row_conversacion);
        View.OnClickListener clickListener = v -> abrirChat(conv);
        if (row != null) {
            row.setOnClickListener(clickListener);
        }
        item.setOnClickListener(clickListener);
    }

    private void abrirChat(Conversacion conv) {
        if (getActivity() == null) {
            return;
        }
        conv.marcarComoLeido();
        Intent intent = new Intent(getActivity(), ChatHotelActivity.class);
        intent.putExtra(ChatHotelActivity.EXTRA_HOTEL_ID, conv.hotelId);
        intent.putExtra(ChatHotelActivity.EXTRA_CONVERSACION_ID, conv.id);
        startActivity(intent);
    }
}
