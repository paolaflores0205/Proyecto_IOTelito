package com.example.proyecto_iotelito.ui.hoteladmin;

import android.Manifest;
import android.content.Context;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.core.view.WindowCompat;
import androidx.lifecycle.LiveData;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.hoteladmin.HotelAdminLocalStore;
import com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository;
import com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelOperations;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminMainBinding;
import com.example.proyecto_iotelito.ui.hoteladmin.hotel.AdminHotelFragment;
import com.example.proyecto_iotelito.ui.hoteladmin.mensajes.AdminMensajesFragment;
import com.example.proyecto_iotelito.ui.hoteladmin.reportes.AdminReportesFragment;
import com.example.proyecto_iotelito.ui.hoteladmin.reservas.AdminReservasFragment;
import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;

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
    private FirebaseHotelOperations operations;
    private LiveData<FirebaseHotelOperations.State<ReservaAdmin>> reservations;
    private final ActivityResultLauncher<String> notificationPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) refreshNotifications();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Los espacios seguros los aplica IoTelitoApplication una sola vez.
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        binding = ActivityHoteladminMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (!HotelAdminAccessGuard.allow(this)) return;
        HotelAdminNotifications.createChannel(this);
        if (FirebaseHotelAdminRepository.remote(this)) {
            operations = new FirebaseHotelOperations(this);
            reservations = operations.reservations();
            reservations.observe(this, state -> {
                refreshNotifications();
                if (state.loading || state.error != null) return;
                long pending = state.rows.stream().filter(row -> row.estado == ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE).count();
                com.google.android.material.badge.BadgeDrawable badge = binding.hoteladminBottomNav.getOrCreateBadge(R.id.nav_hoteladmin_reservas);
                badge.setVisible(pending > 0); badge.setNumber((int)pending);
            });
            configurarNavegacion(savedInstanceState);
            requestNotificationPermission();
            return;
        }
        HotelAdminLocalStore.get(this).load(error -> {
            if (isFinishing() || isDestroyed()) return;
            if (error != null) {
                Toast.makeText(this, R.string.hoteladmin_error_carga_local, Toast.LENGTH_LONG).show();
            }
            SuperadminRepository.getInstance(this).hotel(HotelAdminSampleData.HOTEL_ID)
                    .observe(this, HotelAdminSampleData::aplicarHotelCompartido);
            configurarNavegacion(savedInstanceState);
            refreshNotifications();
            requestNotificationPermission();
        });
    }

    public LiveData<FirebaseHotelOperations.State<ReservaAdmin>> reservations() {
        return reservations;
    }

    private void refreshNotifications() {
        if (operations == null) {
            HotelAdminNotifications.showPendingCheckouts(this, HotelAdminSampleData.reservas());
            return;
        }
        FirebaseHotelOperations.State<ReservaAdmin> state = reservations.getValue();
        if (state != null && !state.loading && state.error == null) {
            HotelAdminNotifications.showRemoteCheckouts(this, operations.uid, operations.hotelId, state.rows);
        }
    }

    private void requestNotificationPermission() {
        if (HotelAdminNotifications.hasPermission(this)) return;
        android.content.SharedPreferences prefs = getSharedPreferences("hoteladmin_alerts", Context.MODE_PRIVATE);
        if (prefs.getBoolean("permission_asked", false)) return;
        prefs.edit().putBoolean("permission_asked", true).apply();
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
    }

    private void configurarNavegacion(Bundle savedInstanceState) {
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

        if (savedInstanceState == null || com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository.remote(this)) {
            openTab(com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository.remote(this)
                    ? R.id.nav_hoteladmin_hotel : getIntent().getIntExtra(EXTRA_OPEN_TAB, R.id.nav_hoteladmin_hotel));
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        openTab(intent.getIntExtra(EXTRA_OPEN_TAB, R.id.nav_hoteladmin_hotel));
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
