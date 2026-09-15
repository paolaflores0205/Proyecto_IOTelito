package com.example.proyecto_iotelito.ui.perfil;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.UserProfile;

/**
 * Pestaña "Perfil": datos del cliente (estáticos en memoria) y accesos a
 * Editar perfil, Notificaciones, Ayuda y Soporte y Cerrar Sesión.
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

        // Botón Editar Perfil
        view.findViewById(R.id.btn_editar_perfil).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), EditarPerfilActivity.class)));

        // Opción Notificaciones
        view.findViewById(R.id.row_notificaciones).setOnClickListener(v ->
                startActivity(new Intent(getActivity(), NotificacionesActivity.class)));

        // Opción Ayuda y Soporte
        view.findViewById(R.id.row_ayuda_soporte).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Centro de Ayuda y Soporte IoTelito", Toast.LENGTH_SHORT).show());

        // Opción Cerrar Sesión
        view.findViewById(R.id.row_cerrar_sesion).setOnClickListener(v ->
                Toast.makeText(requireContext(), "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresca por si el usuario acaba de editar sus datos
        if (getView() != null) {
            pintarDatosPerfil(getView());
        }
    }

    private void pintarDatosPerfil(View view) {
        UserProfile perfil = SampleData.PERFIL;

        ((TextView) view.findViewById(R.id.tv_avatar_inicial)).setText(perfil.initials());
        ((TextView) view.findViewById(R.id.tv_nombre)).setText(perfil.name);
        ((TextView) view.findViewById(R.id.tv_email)).setText(perfil.email);

        // Card Datos Personales
        ((TextView) view.findViewById(R.id.tv_datos_nombre)).setText(perfil.name);
        ((TextView) view.findViewById(R.id.tv_datos_tipo_doc)).setText(perfil.tipoDocumento);
        ((TextView) view.findViewById(R.id.tv_datos_num_doc)).setText(perfil.numeroDocumento);
        ((TextView) view.findViewById(R.id.tv_datos_fecha_nac)).setText(perfil.fechaNacimiento);
        ((TextView) view.findViewById(R.id.tv_datos_email)).setText(perfil.email);
        ((TextView) view.findViewById(R.id.tv_datos_telefono)).setText(perfil.phone);
        ((TextView) view.findViewById(R.id.tv_datos_domicilio)).setText(perfil.domicilio);
    }
}
