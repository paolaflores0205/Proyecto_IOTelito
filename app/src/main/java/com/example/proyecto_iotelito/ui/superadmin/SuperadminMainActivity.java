package com.example.proyecto_iotelito.ui.superadmin;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminMainBinding;
import com.example.proyecto_iotelito.ui.superadmin.dashboard.DashboardFragment;
import com.example.proyecto_iotelito.ui.superadmin.hoteles.HotelesAdminFragment;
import com.example.proyecto_iotelito.ui.superadmin.reportes.ReportesFragment;
import com.example.proyecto_iotelito.ui.superadmin.usuarios.UsuariosFragment;

public class SuperadminMainActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_TAB = "superadmin_open_tab";
    private ActivitySuperadminMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.superadminBottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment;
            int id = item.getItemId();
            if (id == R.id.nav_superadmin_users) fragment = new UsuariosFragment();
            else if (id == R.id.nav_superadmin_hotels) fragment = new HotelesAdminFragment();
            else if (id == R.id.nav_superadmin_reports) fragment = new ReportesFragment();
            else fragment = new DashboardFragment();
            show(fragment);
            return true;
        });

        if (savedInstanceState == null) {
            openTab(getIntent().getIntExtra(EXTRA_OPEN_TAB, R.id.nav_superadmin_home));
        }
    }

    public void openTab(int menuId) {
        binding.superadminBottomNav.setSelectedItemId(menuId);
    }

    private void show(@NonNull Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.superadmin_fragment_container, fragment)
                .commit();
    }
}
