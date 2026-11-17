package com.example.proyecto_iotelito.ui.reservas;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.ReservaRepository;
import com.example.proyecto_iotelito.data.TaxiRepository;
import com.example.proyecto_iotelito.ui.taxi.TaxistaAsignadoActivity;
import com.example.proyecto_iotelito.model.Reserva;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Pestaña "Reservas": segmented control Activas/Finalizadas (Clase 2.3 -
 * Menús / navegación por pestañas). Dentro de "Activas" se agrupa por
 * "Próximas estadías" y "En curso" según la fecha; no existe un estado de
 * "pago pendiente" visible para el cliente, toda reserva activa ya está
 * confirmada.
 */
public class ReservasFragment extends Fragment {

    private TabLayout tabLayout;
    private ReservaAdapter reservaAdapter;
    private TextView vacioView;
    private ReservaRepository reservaRepository;
    private List<Reserva> reservas = new ArrayList<>();
    private LiveData<List<Reserva>> reservasSource;
    private String observedUid;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_reservas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        reservaRepository = ReservaRepository.getInstance(requireContext());
        view.findViewById(R.id.btn_mis_taxis).setOnClickListener(v->startActivity(new Intent(requireContext(),com.example.proyecto_iotelito.ui.taxi.MisTaxisActivity.class)));

        tabLayout = view.findViewById(R.id.tab_layout);
        RecyclerView rvReservas = view.findViewById(R.id.rv_reservas);
        rvReservas.setLayoutManager(new LinearLayoutManager(requireContext()));
        reservaAdapter = new ReservaAdapter(reserva -> abrirDetalle(reserva.id));
        rvReservas.setAdapter(reservaAdapter);
        vacioView = view.findViewById(R.id.tv_reservas_vacio);
        if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null
                && (requireContext().getApplicationInfo().flags
                & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            cargarEjemplos();
        }

        configurarTabs();
        vincularReservas();
        com.example.proyecto_iotelito.data.HotelCatalogRepository.observar(requireContext())
                .observe(getViewLifecycleOwner(), reservaAdapter::setHotels);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                renderReservas(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        renderReservas(0);
    }

    @Override
    public void onResume() {
        super.onResume();
        vincularReservas();
        // refresca por si cambió algo (ej. el estado de una reserva) mientras el usuario estaba en otra pantalla
        configurarTabs();
        renderReservas(tabLayout.getSelectedTabPosition());
    }

    @Override
    public void onDestroyView() {
        reservasSource = null;
        observedUid = null;
        tabLayout = null;
        super.onDestroyView();
    }

    private void cargarEjemplos() {
        androidx.lifecycle.MutableLiveData<Boolean> resultado = new androidx.lifecycle.MutableLiveData<>();
        androidx.lifecycle.LifecycleOwner owner = getViewLifecycleOwner();
        resultado.observe(owner, correcto -> {
            resultado.removeObservers(owner);
            if (correcto) {
                configurarTabs();
                renderReservas(tabLayout.getSelectedTabPosition());
            }
        });
        ReservaRepository repository = reservaRepository;
        com.example.proyecto_iotelito.data.ClientLocalTasks.execute(() -> {
            try {
                repository.asegurarEjemplosPrueba();
                resultado.postValue(true);
            } catch (RuntimeException exception) {
                android.util.Log.e("ReservasFragment", "No se pudieron crear ejemplos", exception);
                resultado.postValue(false);
            }
        });
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        if (!hidden && getView() != null && tabLayout != null) {
            vincularReservas();
            configurarTabs();
            renderReservas(tabLayout.getSelectedTabPosition());
        }
    }

    private void vincularReservas() {
        if (reservaRepository == null || tabLayout == null) return;
        com.google.firebase.auth.FirebaseUser user =
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        String uid = user == null ? null : user.getUid();
        if (reservasSource != null && Objects.equals(observedUid, uid)) return;
        if (reservasSource != null) reservasSource.removeObservers(getViewLifecycleOwner());
        observedUid = uid;
        reservas = new ArrayList<>();
        reservasSource = reservaRepository.observarTodas();
        reservasSource.observe(getViewLifecycleOwner(), rows -> {
            reservas = rows;
            configurarTabs();
            renderReservas(tabLayout.getSelectedTabPosition());
        });
        configurarTabs();
        renderReservas(tabLayout.getSelectedTabPosition());
    }

    private void configurarTabs() {
        int activas = 0;
        int finalizadas = 0;
        for (Reserva reserva : reservas) {
            if (reserva.esActiva()) {
                activas++;
            } else {
                finalizadas++;
            }
        }

        if (tabLayout.getTabCount() == 0) {
            tabLayout.addTab(tabLayout.newTab());
            tabLayout.addTab(tabLayout.newTab());
            tabLayout.addTab(tabLayout.newTab());
        }
        tabLayout.getTabAt(0).setText(getString(R.string.tab_todas_conteo, reservas.size()));
        tabLayout.getTabAt(1).setText(getString(R.string.tab_activas_conteo, activas));
        tabLayout.getTabAt(2).setText(getString(R.string.tab_finalizadas_conteo, finalizadas));
    }

    private void renderReservas(int filtro) {
        List<ReservaAdapter.Item> items = new ArrayList<>();
        List<Reserva> proximas = new ArrayList<>(), enCurso = new ArrayList<>(), pasadas = new ArrayList<>();
        for(Reserva reserva:reservas){
            if(reserva.esActiva()){
                if(filtro==2)continue;
                if(reserva.esProxima()&&reserva.estado!=Reserva.Estado.EN_CHECKOUT)proximas.add(reserva);
                else enCurso.add(reserva);
            }else if(filtro!=1)pasadas.add(reserva);
        }
        proximas.sort(Comparator.comparing(row->row.fechaEntrada));
        enCurso.sort(Comparator.comparing(row->row.fechaEntrada));
        pasadas.sort((a,b)->b.fechaSalida.compareTo(a.fechaSalida));
        if(!proximas.isEmpty()){
            items.add(ReservaAdapter.Item.encabezado(R.string.seccion_proximas_estadias));agregarReservas(items,proximas);
        }
        if(!enCurso.isEmpty()){
            items.add(ReservaAdapter.Item.encabezado(R.string.seccion_en_curso));agregarReservas(items,enCurso);
        }
        if(!pasadas.isEmpty()){
            items.add(ReservaAdapter.Item.encabezado(R.string.seccion_finalizadas_canceladas));agregarReservas(items,pasadas);
        }
        vacioView.setVisibility(items.isEmpty()?View.VISIBLE:View.GONE);
        vacioView.setText(filtro==0?R.string.sin_reservas_todas:filtro==1?R.string.sin_reservas_activas:R.string.sin_reservas_historial);
        reservaAdapter.submitList(items);
    }

    private void agregarReservas(List<ReservaAdapter.Item> items, List<Reserva> reservas) {
        for (Reserva reserva : reservas) {
            items.add(ReservaAdapter.Item.reserva(reserva));
        }
    }

    private void abrirDetalle(int reservaId) {
        startActivity(new Intent(requireContext(), com.example.proyecto_iotelito.ui.reservas.DetalleReservaActivity.class)
                .putExtra(DetalleReservaActivity.EXTRA_RESERVA_ID, reservaId));
    }
}
