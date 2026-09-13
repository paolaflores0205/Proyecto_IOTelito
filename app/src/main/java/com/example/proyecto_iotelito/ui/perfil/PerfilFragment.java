package com.example.proyecto_iotelito.ui.perfil;

import android.content.Intent;
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
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.UserProfile;

/**
 * Pestaña "Perfil": datos del cliente (estáticos, en memoria) y accesos a
 * Editar perfil / Notificaciones.
 */
public class PerfilFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_perfil, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        pintarDatosPerfil(view);

        View rowEditar = view.findViewById(R.id.row_editar_perfil);
        ((ImageView) rowEditar.findViewById(R.id.iv_icono)).setImageResource(R.drawable.ic_edit);
        ((TextView) rowEditar.findViewById(R.id.tv_label)).setText(R.string.menu_editar_perfil);
        rowEditar.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), EditarPerfilActivity.class)));

        View rowNotificaciones = view.findViewById(R.id.row_notificaciones);
        ((ImageView) rowNotificaciones.findViewById(R.id.iv_icono)).setImageResource(R.drawable.ic_notifications);
        ((TextView) rowNotificaciones.findViewById(R.id.tv_label)).setText(R.string.menu_notificaciones);
        rowNotificaciones.setOnClickListener(v ->
                startActivity(new Intent(getActivity(), NotificacionesActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        // refresca por si el usuario acaba de editar su perfil
        pintarDatosPerfil(requireView());
    }

    private void pintarDatosPerfil(View view) {
        UserProfile perfil = SampleData.PERFIL;
        ((TextView) view.findViewById(R.id.tv_avatar_inicial)).setText(perfil.initials());
        ((TextView) view.findViewById(R.id.tv_nombre)).setText(perfil.name);
        ((TextView) view.findViewById(R.id.tv_email)).setText(perfil.email);
        ((TextView) view.findViewById(R.id.tv_telefono)).setText(perfil.phone);
    }
}
