package com.example.proyecto_iotelito;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.example.proyecto_iotelito.ui.explorar.ExplorarFragment;
import com.example.proyecto_iotelito.ui.mensajes.MensajesFragment;
import com.example.proyecto_iotelito.ui.perfil.PerfilFragment;
import com.example.proyecto_iotelito.ui.reservas.ReservasFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Shell principal de la app del cliente: aloja la navegación inferior
 * (Explorar / Reservas / Mensajes / Perfil) descrita en los mockups y
 * conmuta entre sus fragments sin recrearlos en cada toque.
 */
public class MainActivity extends AppCompatActivity {

    /** Extra para abrir el shell directamente en una pestaña específica, ej. desde ReservaConfirmadaActivity. */
    public static final String EXTRA_OPEN_TAB = "extra_open_tab";

    private static final String TAG_EXPLORAR = "tab_explorar";
    private static final String TAG_RESERVAS = "tab_reservas";
    private static final String TAG_MENSAJES = "tab_mensajes";
    private static final String TAG_PERFIL = "tab_perfil";

    private Fragment explorarFragment;
    private Fragment reservasFragment;
    private Fragment mensajesFragment;
    private Fragment perfilFragment;
    private Fragment activeFragment;

    private interface FragmentFactory {
        Fragment create();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FragmentManager fm = getSupportFragmentManager();
        explorarFragment = findOrCreate(fm, TAG_EXPLORAR, ExplorarFragment::new);
        reservasFragment = findOrCreate(fm, TAG_RESERVAS, ReservasFragment::new);
        mensajesFragment = findOrCreate(fm, TAG_MENSAJES, MensajesFragment::new);
        perfilFragment = findOrCreate(fm, TAG_PERFIL, PerfilFragment::new);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_explorar) {
                showFragment(explorarFragment);
                return true;
            } else if (id == R.id.nav_reservas) {
                showFragment(reservasFragment);
                return true;
            } else if (id == R.id.nav_mensajes) {
                showFragment(mensajesFragment);
                return true;
            } else if (id == R.id.nav_perfil) {
                showFragment(perfilFragment);
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            int tabToOpen = getIntent().getIntExtra(EXTRA_OPEN_TAB, R.id.nav_explorar);
            bottomNav.setSelectedItemId(tabToOpen);
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        int tabToOpen = intent.getIntExtra(EXTRA_OPEN_TAB, R.id.nav_explorar);
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(tabToOpen);
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
