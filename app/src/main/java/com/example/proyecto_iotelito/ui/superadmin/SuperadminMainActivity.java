package com.example.proyecto_iotelito.ui.superadmin;

import android.os.Bundle;
import android.Manifest;
import android.content.Context;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.lifecycle.ViewModelProvider;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminMainBinding;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.notification.SuperadminNotifications;
import com.google.android.material.snackbar.Snackbar;

public class SuperadminMainActivity extends AppCompatActivity {
    public static final String EXTRA_OPEN_TAB = "superadmin_open_tab";
    private ActivitySuperadminMainBinding binding;
    private NavController navController;
    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) SuperadminRepository.getInstance(this).publishPendingAlerts();
            });

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
        SuperadminNotifications.createChannel(this);
        SuperadminViewModel model = new ViewModelProvider(this).get(SuperadminViewModel.class);
        model.ready().observe(this, ready -> {
            if (Boolean.TRUE.equals(ready)) SuperadminRepository.getInstance(this).publishPendingAlerts();
        });
        model.error().observe(this, error -> {
            if (error != null) Snackbar.make(binding.getRoot(), error, Snackbar.LENGTH_INDEFINITE).show();
        });
        android.content.SharedPreferences prefs = getSharedPreferences("superadmin_settings", Context.MODE_PRIVATE);
        if (!SuperadminNotifications.hasPermission(this) && !prefs.getBoolean("notification_permission_asked", false)) {
            prefs.edit().putBoolean("notification_permission_asked", true).apply();
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        SuperadminRepository.getInstance(this).publishPendingAlerts();
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
