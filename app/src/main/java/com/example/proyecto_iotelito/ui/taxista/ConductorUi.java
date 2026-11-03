package com.example.proyecto_iotelito.ui.taxista;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.taxista.DisponibilidadEstado;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.io.File;

final class ConductorUi {
    static void disponibilidad(Fragment f,View root,int switchId,int textId,TaxistaViewModel vm) {
        MaterialSwitch toggle=root.findViewById(switchId); TextView label=root.findViewById(textId);
        vm.disponibilidad.estado().observe(f.getViewLifecycleOwner(),estado->{
            // Desconectar el callback evita escrituras al reflejar cambios de Firestore en ambos menús.
            toggle.setOnCheckedChangeListener(null);
            toggle.setChecked(estado.disponible);
            toggle.setEnabled(estado.editable());
            int texto=estado.disponible?R.string.taxi_disponible_desc:R.string.taxi_no_disponible_desc;
            if(estado.guardando) texto=R.string.taxista_disponibilidad_guardando;
            else switch(estado.problema){
                case CARGANDO: texto=R.string.taxista_disponibilidad_cargando;break;
                case CONEXION: texto=R.string.taxista_disponibilidad_conexion;break;
                case PERMISOS: texto=R.string.taxista_disponibilidad_permisos;break;
                case PERFIL: texto=R.string.taxista_disponibilidad_perfil;break;
                case SESION: texto=R.string.taxista_disponibilidad_sesion;break;
                case GUARDADO: texto=R.string.taxista_disponibilidad_error;break;
                default: break;
            }
            label.setText(texto);
            toggle.setOnCheckedChangeListener((button,enabled)->vm.disponibilidad.cambiar(enabled));
            if(estado.editable())vm.repo.refrescar();
        });
    }
    static void foto(Fragment f,View root,TaxistaViewModel vm){foto(f.requireActivity(),f.getViewLifecycleOwner(),root,vm);}
    static void foto(androidx.appcompat.app.AppCompatActivity a,View root,TaxistaViewModel vm){foto(a,a,root,vm);}
    private static void foto(android.content.Context c,LifecycleOwner owner,View root,TaxistaViewModel vm){
        TextView initials=root.findViewById(R.id.tv_avatar_inicial);if(initials==null)return;
        android.view.ViewParent parent=initials.getParent();if(!(parent instanceof FrameLayout))return;
        FrameLayout frame=(FrameLayout)parent;frame.setClipToOutline(true);
        ImageView old=frame.findViewWithTag("taxista_foto");
        if(old!=null)frame.removeView(old);
        initials.setVisibility(View.VISIBLE);
        vm.repo.ejecutar(owner,()->{
            File file=ConductorFoto.archivo(c,vm.repo.cuenta);if(!file.exists())return null;
            BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
            return BitmapFactory.decodeFile(file.getAbsolutePath(),options);
        },bitmap->{
            if(bitmap==null)return;
            ImageView image=new ImageView(c);image.setTag("taxista_foto");image.setContentDescription(c.getString(R.string.taxista_local_foto));
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);image.setImageBitmap(bitmap);
            frame.addView(image,new FrameLayout.LayoutParams(-1,-1));initials.setVisibility(View.GONE);
        });
    }
}
