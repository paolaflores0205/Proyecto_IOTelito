package com.example.proyecto_iotelito.ui.taxista;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.*;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.taxista.ConductorPreferencias;
import com.example.proyecto_iotelito.data.taxista.local.ServicioConductorEntity;
import com.google.android.material.button.MaterialButton;
import java.time.*;
import java.util.*;

/** Inicio prioriza el viaje activo; avisos y solicitudes se calculan con los datos guardados. */
public class InicioTaxistaFragment extends Fragment {
    private TaxistaViewModel vm;
    private List<ServicioConductorEntity> servicios=Collections.emptyList();
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup container,@Nullable Bundle state){
        return inflater.inflate(R.layout.fragment_inicio_taxista,container,false);
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
        super.onViewCreated(view,state);
        vm=new ViewModelProvider(requireActivity()).get(TaxistaViewModel.class);
        ((TextView)view.findViewById(R.id.tv_saludo)).setText(getString(R.string.taxi_saludo,"Miguel"));
        ((TextView)view.findViewById(R.id.tv_avatar_inicial)).setText("MA");
        ((TextView)view.findViewById(R.id.tv_nombre_conductor)).setText("Miguel Arispe S.");
        ((TextView)view.findViewById(R.id.tv_rating_vehiculo)).setText("★ 4.8 · Toyota Corolla (ABC-123)");
        ((TextView)view.findViewById(R.id.tv_stat_calificacion)).setText("4.8");
        ConductorUi.disponibilidad(this,view,R.id.switch_disponibilidad,R.id.tv_estado_disponibilidad,vm);
        vm.repo.servicios.observe(getViewLifecycleOwner(),rows->{servicios=rows;pintar(view);});
        vm.repo.avisos.observe(getViewLifecycleOwner(),rows->{
            int nuevos=0;for(com.example.proyecto_iotelito.data.taxista.local.AvisoConductorEntity aviso:rows)if(!aviso.leido)nuevos++;
            TextView badge=view.findViewById(R.id.taxista_badge_avisos);
            badge.setText(String.valueOf(nuevos));badge.setVisibility(nuevos==0?View.GONE:View.VISIBLE);
            view.findViewById(R.id.taxista_abrir_avisos).setContentDescription(getString(R.string.taxista_ux_avisos_pendientes,nuevos));
        });
        view.findViewById(R.id.taxista_abrir_avisos).setOnClickListener(v->{
            if(getParentFragmentManager().findFragmentByTag("taxista_avisos")==null)
                new TaxistaAvisosSheet().show(getParentFragmentManager(),"taxista_avisos");
        });
        view.findViewById(R.id.btn_ver_solicitudes).setOnClickListener(v->irASolicitudes());
        view.findViewById(R.id.banner_nueva_solicitud).setOnClickListener(v->irASolicitudes());
        prefs=ConductorPreferencias.get(requireContext(),vm.repo.cuenta);
        listener=(p,key)->{if("disponible".equals(key)&&getView()!=null)pintar(requireView());};
        prefs.registerOnSharedPreferenceChangeListener(listener);
        pintar(view);
    }
    private void pintar(View view){
        int pendientes=0,completados=0;
        long hoy=LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        ServicioConductorEntity activo=null,proximo=null;
        for(ServicioConductorEntity e:servicios){
            if("SOLICITADO".equals(e.estado)){
                pendientes++;if(proximo==null||e.minutos<proximo.minutos)proximo=e;
            }else if("ASIGNADO".equals(e.estado)||"EN_CAMINO".equals(e.estado)||"EN_TRASLADO".equals(e.estado))activo=e;
            if("FINALIZADO".equals(e.estado)&&e.finalizado>=hoy)completados++;
        }
        ((TextView)view.findViewById(R.id.tv_stat_servicios)).setText(String.valueOf(completados+(activo==null?0:1)));
        ((TextView)view.findViewById(R.id.tv_stat_trasladados)).setText(String.valueOf(completados));
        ((MaterialButton)view.findViewById(R.id.btn_ver_solicitudes)).setText(getString(R.string.taxi_btn_ver_solicitudes,pendientes));
        view.findViewById(R.id.taxista_card_activo).setVisibility(activo==null?View.GONE:View.VISIBLE);
        if(activo!=null){
            ServicioConductorEntity viaje=activo;
            ((TextView)view.findViewById(R.id.taxista_activo_pasajero)).setText(viaje.nombre);
            ((TextView)view.findViewById(R.id.taxista_activo_ruta)).setText(getString(R.string.taxista_ux_ruta,viaje.hotel,viaje.destino));
            ((TextView)view.findViewById(R.id.taxista_activo_estado)).setText(EstadoServicioUi.texto(requireContext(),EstadoServicio.valueOf(viaje.estado)));
            view.findViewById(R.id.taxista_continuar).setOnClickListener(v->TaxistaFlujo.abrir(requireContext(),viaje));
        }
        boolean mostrar=activo==null&&proximo!=null&&ConductorPreferencias.disponible(requireContext(),vm.repo.cuenta);
        view.findViewById(R.id.banner_nueva_solicitud).setVisibility(mostrar?View.VISIBLE:View.GONE);
        if(proximo!=null){
            ((TextView)view.findViewById(R.id.taxista_banner_titulo)).setText(getResources().getQuantityString(R.plurals.taxista_ux_solicitudes,pendientes,pendientes));
            ((TextView)view.findViewById(R.id.taxista_banner_detalle)).setText(getString(R.string.taxista_ux_pedido,proximo.hotel,proximo.nombre));
        }
    }
    @Override public void onResume(){super.onResume();if(vm!=null){vm.repo.refrescar();ConductorUi.foto(this,requireView(),vm);}}
    @Override public void onHiddenChanged(boolean hidden){super.onHiddenChanged(hidden);if(!hidden&&vm!=null)vm.repo.refrescar();}
    @Override public void onDestroyView(){if(prefs!=null&&listener!=null)prefs.unregisterOnSharedPreferenceChangeListener(listener);servicios=Collections.emptyList();super.onDestroyView();}
    private void irASolicitudes(){if(getActivity() instanceof TaxistaMainActivity)((TaxistaMainActivity)getActivity()).selectTab(R.id.nav_taxi_solicitudes);}
}
