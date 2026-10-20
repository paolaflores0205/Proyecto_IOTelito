package com.example.proyecto_iotelito.ui.taxista;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import androidx.annotation.*;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.taxista.local.AvisoConductorEntity;
import java.text.DateFormat;
import java.util.*;

public class TaxistaAvisosSheet extends BottomSheetDialogFragment {
    private TaxistaViewModel vm;
    @Nullable @Override public View onCreateView(@NonNull LayoutInflater inflater,@Nullable ViewGroup parent,@Nullable Bundle state){
        return inflater.inflate(R.layout.sheet_taxista_avisos,parent,false);
    }
    @Override public void onViewCreated(@NonNull View view,@Nullable Bundle state){
        vm=new ViewModelProvider(requireActivity()).get(TaxistaViewModel.class);
        RecyclerView list=view.findViewById(R.id.taxista_lista_avisos);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));AvisosAdapter adapter=new AvisosAdapter();list.setAdapter(adapter);
        vm.repo.avisos.observe(getViewLifecycleOwner(),rows->{adapter.rows=rows;adapter.notifyDataSetChanged();view.findViewById(R.id.taxista_avisos_vacio).setVisibility(rows.isEmpty()?View.VISIBLE:View.GONE);});
        view.findViewById(R.id.taxista_configurar_avisos).setOnClickListener(v->startActivity(new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE,requireContext().getPackageName()).putExtra(Settings.EXTRA_CHANNEL_ID,TaxistaNotificaciones.CHANNEL)));
    }
    private class AvisosAdapter extends RecyclerView.Adapter<AvisoHolder>{
        List<AvisoConductorEntity> rows=new ArrayList<>();
        @Override public int getItemCount(){return rows.size();}
        @NonNull @Override public AvisoHolder onCreateViewHolder(@NonNull ViewGroup p,int type){return new AvisoHolder(LayoutInflater.from(p.getContext()).inflate(R.layout.item_taxista_aviso,p,false));}
        @Override public void onBindViewHolder(@NonNull AvisoHolder h,int position){
            AvisoConductorEntity a=rows.get(position);
            ((TextView)h.itemView.findViewById(R.id.taxista_aviso_titulo)).setText(a.titulo);
            ((TextView)h.itemView.findViewById(R.id.taxista_aviso_texto)).setText(a.texto);
            ((TextView)h.itemView.findViewById(R.id.taxista_aviso_fecha)).setText(DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(new Date(a.fecha)));
            h.itemView.setAlpha(a.leido?0.65f:1f);
            h.itemView.setOnClickListener(v->{vm.repo.leer(a.clave);
                if("finalizado".equals(a.tipo))ResumenServicioSheet.newInstance(a.servicio).show(getParentFragmentManager(),ResumenServicioSheet.TAG);
                else vm.repo.ejecutar(getViewLifecycleOwner(),()->vm.repo.buscar(a.servicio),e->{if(e!=null)TaxistaFlujo.abrir(requireContext(),e);});
            });
        }
    }
    private static class AvisoHolder extends RecyclerView.ViewHolder{AvisoHolder(View v){super(v);}}
}
