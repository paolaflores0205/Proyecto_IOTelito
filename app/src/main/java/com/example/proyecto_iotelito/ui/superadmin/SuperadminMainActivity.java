package com.example.proyecto_iotelito.ui.superadmin;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminMainBinding;

public class SuperadminMainActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_TAB = "superadmin_open_tab";
    private ActivitySuperadminMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.superadmin_fragment_container);
        if (navHost == null) throw new IllegalStateException("NavHost de superadmin no encontrado");
        navController = navHost.getNavController();
        binding.navSuperadminHomeButton.setOnClickListener(v -> openTab(R.id.nav_superadmin_home));
        binding.navSuperadminUsersButton.setOnClickListener(v -> openTab(R.id.nav_superadmin_users));
        binding.navSuperadminHotelsButton.setOnClickListener(v -> openTab(R.id.nav_superadmin_hotels));
        binding.navSuperadminReportsButton.setOnClickListener(v -> openTab(R.id.nav_superadmin_reports));
        navController.addOnDestinationChangedListener((controller, destination, arguments) ->
                updateSelectedButton(destination.getId()));

        if (savedInstanceState == null) {
            openTab(getIntent().getIntExtra(EXTRA_OPEN_TAB, R.id.nav_superadmin_home));
        }
    }

    public void openTab(int menuId) {
        if (navController.getCurrentDestination() == null
                || navController.getCurrentDestination().getId() != menuId) {
            navController.navigate(menuId);
        }
        updateSelectedButton(menuId);
    }

    private void updateSelectedButton(int destinationId) {
        binding.navSuperadminHomeButton.setSelected(destinationId == R.id.nav_superadmin_home);
        binding.navSuperadminUsersButton.setSelected(destinationId == R.id.nav_superadmin_users);
        binding.navSuperadminHotelsButton.setSelected(destinationId == R.id.nav_superadmin_hotels);
        binding.navSuperadminReportsButton.setSelected(destinationId == R.id.nav_superadmin_reports);
    }

}
