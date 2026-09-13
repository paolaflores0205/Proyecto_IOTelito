package com.example.proyecto_iotelito.ui.mensajes;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;

/**
 * Pestaña "Mensajes". El chat con el hotel se habilitará cuando exista
 * una reserva activa (siguiente incremento).
 */
public class MensajesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_placeholder, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ((ImageView) view.findViewById(R.id.iv_placeholder_icon)).setImageResource(R.drawable.ic_chat);
        ((TextView) view.findViewById(R.id.tv_placeholder_title)).setText(R.string.mensajes_titulo);
        ((TextView) view.findViewById(R.id.tv_placeholder_message)).setText(R.string.mensajes_placeholder);
    }
}
