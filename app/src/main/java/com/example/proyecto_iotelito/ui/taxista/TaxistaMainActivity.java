package com.example.proyecto_iotelito.ui.taxista;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.proyecto_iotelito.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Shell principal de la app del taxista: aloja la navegación inferior
 * (Inicio / Solicitudes / Historial / Perfil) y conmuta entre sus fragments
 * sin recrearlos en cada toque, igual que {@link com.example.proyecto_iotelito.MainActivity}
 * lo hace para el rol cliente.
 */
public class TaxistaMainActivity extends AppCompatActivity {

    /** Extra para abrir el shell directamente en una pestaña específica, ej. desde ServicioFinalizadoActivity. */
    public static final String EXTRA_OPEN_TAB = "extra_open_tab";

    private static final String TAG_INICIO = "tab_taxi_inicio";
    private static final String TAG_SOLICITUDES = "tab_taxi_solicitudes";
    private static final String TAG_HISTORIAL = "tab_taxi_historial";
    private static final String TAG_PERFIL = "tab_taxi_perfil";

    private Fragment inicioFragment;
    private Fragment solicitudesFragment;
    private Fragment historialFragment;
    private Fragment perfilFragment;
    private Fragment activeFragment;

    private BottomNavigationView bottomNav;
    private TaxistaViewModel vm;
    private final androidx.activity.result.ActivityResultLauncher<String> permiso=registerForActivityResult(
        new androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),granted->{if(vm!=null)vm.repo.refrescar();});

    private interface FragmentFactory {
        Fragment create();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!TaxistaFlujo.sesion(this))return;
        setContentView(R.layout.activity_taxista_main);
        vm=TaxistaFlujo.model(this);
        TaxistaNotificaciones.canal(this);

        FragmentManager fm = getSupportFragmentManager();
        inicioFragment = findOrCreate(fm, TAG_INICIO, InicioTaxistaFragment::new);
        solicitudesFragment = findOrCreate(fm, TAG_SOLICITUDES, SolicitudesTaxistaFragment::new);
        historialFragment = findOrCreate(fm, TAG_HISTORIAL, HistorialTaxistaFragment::new);
        perfilFragment = findOrCreate(fm, TAG_PERFIL, PerfilTaxistaFragment::new);

        bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_taxi_inicio) {
                showFragment(inicioFragment);
                return true;
            } else if (id == R.id.nav_taxi_solicitudes) {
                showFragment(solicitudesFragment);
                return true;
            } else if (id == R.id.nav_taxi_historial) {
                showFragment(historialFragment);
                return true;
            } else if (id == R.id.nav_taxi_perfil) {
                showFragment(perfilFragment);
                return true;
            }
            return false;
        });

        int tabToOpen=savedInstanceState==null?getIntent().getIntExtra(EXTRA_OPEN_TAB,R.id.nav_taxi_inicio)
                :savedInstanceState.getInt(EXTRA_OPEN_TAB,R.id.nav_taxi_inicio);
        for(Fragment f:new Fragment[]{inicioFragment,solicitudesFragment,historialFragment,perfilFragment})
            getSupportFragmentManager().beginTransaction().hide(f).commitNow();
        bottomNav.setSelectedItemId(tabToOpen);
        procesarAviso(getIntent());
        android.content.SharedPreferences prefs=com.example.proyecto_iotelito.data.taxista.ConductorPreferencias.get(this,vm.repo.cuenta);
        if(!TaxistaNotificaciones.permiso(this)&&!prefs.getBoolean("permission_asked",false)){
            prefs.edit().putBoolean("permission_asked",true).apply();permiso.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int tabToOpen = intent.getIntExtra(EXTRA_OPEN_TAB, R.id.nav_taxi_inicio);
        bottomNav.setSelectedItemId(tabToOpen);
        procesarAviso(intent);
    }

    @Override protected void onResume(){super.onResume();if(vm!=null)vm.repo.refrescar();}
    @Override protected void onSaveInstanceState(@NonNull Bundle state){
        if(bottomNav!=null)state.putInt(EXTRA_OPEN_TAB,bottomNav.getSelectedItemId());super.onSaveInstanceState(state);
    }
    private void procesarAviso(Intent intent){
        String clave=intent.getStringExtra("taxista_aviso_clave");if(clave==null||vm==null)return;
        String cuenta=intent.getStringExtra("taxista_aviso_cuenta");
        if(!vm.repo.cuenta.equals(cuenta))return;
        int id=intent.getIntExtra("taxista_aviso_id",0);
        String tipo=intent.getStringExtra("taxista_aviso_tipo");
        intent.removeExtra("taxista_aviso_clave");vm.repo.leer(clave);
        if("finalizado".equals(tipo)){bottomNav.setSelectedItemId(R.id.nav_taxi_historial);}
        else vm.repo.ejecutar(this,()->vm.repo.buscar(id),e->{if(e!=null)TaxistaFlujo.abrir(this,e);});
    }

    /** Permite que un fragment hijo (ej. Inicio) cambie de pestaña sin pasar por un Intent. */
    void selectTab(int menuItemId) {
        bottomNav.setSelectedItemId(menuItemId);
    }

    private Fragment findOrCreate(FragmentManager fm, String tag, FragmentFactory factory) {
        Fragment fragment = fm.findFragmentByTag(tag);
        if (fragment == null) {
            fragment = factory.create();
            fm.beginTransaction()
                    .add(R.id.fragment_container, fragment, tag)
                    .hide(fragment)
                    .commitNow();
        }
        return fragment;
    }

    private void showFragment(@NonNull Fragment fragment) {
        if (fragment == activeFragment) {
            return;
        }
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (activeFragment != null) {
            transaction.hide(activeFragment);
        }
        transaction.show(fragment);
        transaction.commit();
        activeFragment = fragment;
    }
}
