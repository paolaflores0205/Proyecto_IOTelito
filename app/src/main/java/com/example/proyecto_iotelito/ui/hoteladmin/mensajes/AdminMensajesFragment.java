package com.example.proyecto_iotelito.ui.hoteladmin.mensajes;

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
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.databinding.FragmentHoteladminMensajesBinding;
import com.example.proyecto_iotelito.model.hoteladmin.Conversacion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pantalla adm-bandeja-mensajes con RecyclerView. */
public class AdminMensajesFragment extends Fragment {
    private FragmentHoteladminMensajesBinding binding;
    private ConversacionesAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHoteladminMensajesBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        adapter = new ConversacionesAdapter(this::abrirChat);
        binding.rvConversaciones.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvConversaciones.setAdapter(adapter);
        binding.etBuscar.addTextChangedListener(new SimpleTextWatcher(this::renderConversaciones));
        binding.chipGroupFiltro.setOnCheckedStateChangeListener((group, ids) -> renderConversaciones());
        binding.chipTodos.setChecked(true);
        renderConversaciones();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (binding != null) renderConversaciones();
    }

    private void renderConversaciones() {
        String query = String.valueOf(binding.etBuscar.getText()).trim().toLowerCase(Locale.ROOT);
        boolean soloHospedados = binding.chipHospedados.isChecked();
        List<Conversacion> visibles = new ArrayList<>();
        for (Conversacion conversacion : HotelAdminSampleData.conversaciones()) {
            String searchable = (conversacion.clienteNombre + " " + conversacion.habitacion + " "
                    + conversacion.ultimoMensaje).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            if (soloHospedados && !conversacion.hospedado) continue;

            visibles.add(conversacion);
        }
        adapter.setConversaciones(visibles);
        binding.tvSinConversaciones.setVisibility(visibles.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void abrirChat(Conversacion conversacion) {
        conversacion.noLeidos = 0;
        Intent intent = new Intent(requireContext(), AdminChatActivity.class);
        intent.putExtra(AdminChatActivity.EXTRA_CONVERSACION_ID, conversacion.id);
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding.rvConversaciones.setAdapter(null);
        adapter = null;
        binding = null;
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable after;
        SimpleTextWatcher(Runnable after) { this.after = after; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override public void afterTextChanged(Editable s) { after.run(); }
    }
}
