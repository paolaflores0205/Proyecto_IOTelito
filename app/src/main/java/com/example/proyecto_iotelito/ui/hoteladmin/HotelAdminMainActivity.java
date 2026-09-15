package com.example.proyecto_iotelito.ui.hoteladmin;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminMainBinding;
import com.example.proyecto_iotelito.ui.hoteladmin.hotel.AdminHotelFragment;
import com.example.proyecto_iotelito.ui.hoteladmin.mensajes.AdminMensajesFragment;
import com.example.proyecto_iotelito.ui.hoteladmin.reportes.AdminReportesFragment;
import com.example.proyecto_iotelito.ui.hoteladmin.reservas.AdminReservasFragment;

/**
 * Shell principal del panel de Administrador de hotel (rol HOTEL_ADMIN,
 * sección 02 del Figma). Aloja la navegación inferior
 * (Hotel / Reservas / Mensajes / Reportes) y conmuta entre sus fragments.
 *
 * Sigue el mismo patrón que {@link com.example.proyecto_iotelito.ui.superadmin.SuperadminMainActivity}
 * (View Binding + replace()). Se llega aquí desde el login por rol.
 */
public class HotelAdminMainActivity extends AppCompatActivity {

    public static final String EXTRA_OPEN_TAB = "hoteladmin_open_tab";
    private ActivityHoteladminMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHoteladminMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.hoteladminBottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_hoteladmin_reservas) fragment = new AdminReservasFragment();
            else if (id == R.id.nav_hoteladmin_mensajes) fragment = new AdminMensajesFragment();
            else if (id == R.id.nav_hoteladmin_reportes) fragment = new AdminReportesFragment();
            else fragment = new AdminHotelFragment();
            show(fragment);
            return true;
        });

        if (savedInstanceState == null) {
            openTab(getIntent().getIntExtra(EXTRA_OPEN_TAB, R.id.nav_hoteladmin_hotel));
        }
    }

    public void openTab(int menuId) {
        binding.hoteladminBottomNav.setSelectedItemId(menuId);
    }

    private void show(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.hoteladmin_fragment_container, fragment)
                .commit();
    }
}
