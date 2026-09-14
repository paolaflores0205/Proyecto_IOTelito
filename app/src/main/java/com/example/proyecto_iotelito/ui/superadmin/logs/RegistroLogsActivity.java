package com.example.proyecto_iotelito.ui.superadmin.logs;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SuperadminSampleData;
import com.example.proyecto_iotelito.databinding.ActivitySuperadminRegistroLogsBinding;
import com.example.proyecto_iotelito.databinding.ItemSuperadminLogBinding;
import com.example.proyecto_iotelito.model.superadmin.ActivityLog;

import java.util.Locale;

public class RegistroLogsActivity extends AppCompatActivity {
    private ActivitySuperadminRegistroLogsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySuperadminRegistroLogsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable s) { renderLogs(); }
        });
        binding.chipGroupType.setOnCheckedStateChangeListener((group, checkedIds) -> renderLogs());
        renderLogs();
    }

    private void renderLogs() {
        String query = String.valueOf(binding.etSearch.getText()).trim().toLowerCase(Locale.ROOT);
        String filter = selectedFilter();
        binding.logListContainer.removeAllViews();
        for (ActivityLog log : SuperadminSampleData.logs()) {
            String searchable = (log.type + " " + log.title + " " + log.detail).toLowerCase(Locale.ROOT);
            if (!query.isEmpty() && !searchable.contains(query)) continue;
            if (filter != null && !filter.equals(log.type)) continue;
            ItemSuperadminLogBinding item = ItemSuperadminLogBinding.inflate(getLayoutInflater(), binding.logListContainer, false);
            item.tvType.setText(log.type);
            item.tvTitle.setText(log.title);
            item.tvDetail.setText(log.detail);
            item.tvTime.setText(log.time);
            int color = log.type.equals("Alerta") ? R.color.io_danger_text : (log.type.equals("Sistema") ? R.color.io_navy : R.color.io_teal);
            item.tvType.setTextColor(ContextCompat.getColor(this, color));
            binding.logListContainer.addView(item.getRoot());
        }
    }

    private String selectedFilter() {
        int id = binding.chipGroupType.getCheckedChipId();
        if (id == R.id.chip_alert) return "Alerta";
        if (id == R.id.chip_system) return "Sistema";
        return null;
    }
}
