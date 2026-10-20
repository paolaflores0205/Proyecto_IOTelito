package com.example.proyecto_iotelito.ui.taxista;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.*;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.taxista.ConductorPreferencias;
import com.google.android.material.materialswitch.MaterialSwitch;
import java.io.File;

final class ConductorUi {
    static void disponibilidad(Fragment f,View root,int switchId,int textId,TaxistaViewModel vm) {
        SharedPreferences prefs=ConductorPreferencias.get(f.requireContext(),vm.repo.cuenta);
        MaterialSwitch toggle=root.findViewById(switchId); TextView label=root.findViewById(textId);
        Runnable refresh=()->{
            boolean enabled=prefs.getBoolean("disponible",true);
            toggle.setChecked(enabled);label.setText(enabled?R.string.taxi_disponible_desc:R.string.taxi_no_disponible_desc);
        };
        refresh.run();
        toggle.setOnCheckedChangeListener((button,enabled)->{
            if(prefs.getBoolean("disponible",true)!=enabled){prefs.edit().putBoolean("disponible",enabled).apply();vm.repo.refrescar();}
            label.setText(enabled?R.string.taxi_disponible_desc:R.string.taxi_no_disponible_desc);
        });
        SharedPreferences.OnSharedPreferenceChangeListener listener=(p,key)->{if("disponible".equals(key))refresh.run();};
        prefs.registerOnSharedPreferenceChangeListener(listener);
        f.getViewLifecycleOwner().getLifecycle().addObserver((LifecycleEventObserver)(owner,event)->{
            if(event==Lifecycle.Event.ON_DESTROY)prefs.unregisterOnSharedPreferenceChangeListener(listener);
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
