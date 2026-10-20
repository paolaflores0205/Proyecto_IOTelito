package com.example.proyecto_iotelito.ui.superadmin.notificaciones;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.proyecto_iotelito.data.superadmin.SuperadminRepository;
import com.example.proyecto_iotelito.data.superadmin.local.AdminAlertEntity;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminAvisosBinding;
import com.example.proyecto_iotelito.databinding.ItemSuperadminLogBinding;
import com.example.proyecto_iotelito.notification.SuperadminNotifications;
import com.example.proyecto_iotelito.ui.superadmin.hoteles.DetalleHotelAdminActivity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AvisosSuperadminActivity extends AppCompatActivity {
    private ActivitySuperadminAvisosBinding binding;
    private final List<AdminAlertEntity> alerts = new ArrayList<>();
    private final ActivityResultLauncher<String> permission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) SuperadminRepository.getInstance(this).publishPendingAlerts();
            });

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        binding = ActivitySuperadminAvisosBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        AlertAdapter adapter = new AlertAdapter();
        binding.rvAlerts.setLayoutManager(new LinearLayoutManager(this));
        binding.rvAlerts.setAdapter(adapter);
        SuperadminRepository.getInstance(this).alerts().observe(this, rows -> {
            alerts.clear();
            alerts.addAll(rows);
            adapter.notifyDataSetChanged();
            binding.tvEmpty.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        });
        binding.btnPermission.setOnClickListener(v -> {
            if (!SuperadminNotifications.hasPermission(this) && shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                permission.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName()));
            }
        });
    }

    @Override protected void onResume() {
        super.onResume();
        SuperadminRepository.getInstance(this).publishPendingAlerts();
    }

    private class AlertAdapter extends RecyclerView.Adapter<AlertHolder> {
        @NonNull @Override public AlertHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            return new AlertHolder(ItemSuperadminLogBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
        @Override public int getItemCount() { return alerts.size(); }
        @Override public void onBindViewHolder(@NonNull AlertHolder holder, int position) {
            AdminAlertEntity alert = alerts.get(position);
            holder.item.tvType.setText(alert.read ? "Leído" : "Pendiente");
            holder.item.tvTitle.setText(alert.title);
            holder.item.tvDetail.setText(alert.message);
            holder.item.tvTime.setText(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(new Date(alert.createdAt)));
            holder.item.getRoot().setOnClickListener(v -> {
                SuperadminRepository.getInstance(AvisosSuperadminActivity.this).markAlertsRead(alert.hotelId);
                startActivity(new Intent(AvisosSuperadminActivity.this, DetalleHotelAdminActivity.class)
                        .putExtra(DetalleHotelAdminActivity.EXTRA_HOTEL_ID, alert.hotelId));
            });
        }
    }
    private static class AlertHolder extends RecyclerView.ViewHolder {
        final ItemSuperadminLogBinding item;
        AlertHolder(ItemSuperadminLogBinding item) { super(item.getRoot()); this.item = item; }
    }
}
