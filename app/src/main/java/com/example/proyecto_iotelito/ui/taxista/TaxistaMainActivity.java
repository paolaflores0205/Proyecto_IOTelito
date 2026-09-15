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

    private interface FragmentFactory {
        Fragment create();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taxista_main);

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

        if (savedInstanceState == null) {
            int tabToOpen = getIntent().getIntExtra(EXTRA_OPEN_TAB, R.id.nav_taxi_inicio);
            bottomNav.setSelectedItemId(tabToOpen);
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int tabToOpen = intent.getIntExtra(EXTRA_OPEN_TAB, R.id.nav_taxi_inicio);
        bottomNav.setSelectedItemId(tabToOpen);
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
