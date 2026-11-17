package com.example.proyecto_iotelito.ui.taxi;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.ClientTaxiRepository;
import com.example.proyecto_iotelito.data.TaxiRepository;
import com.example.proyecto_iotelito.data.taxiapi.TaxiTasks;
import java.util.Collections;
import java.util.List;
import org.json.JSONObject;

/** Solicitudes propias con el mismo diseño y navegación del resto del cliente. */
public class MisTaxisActivity extends AppCompatActivity {
    private LinearLayout list;
    private TextView status;
    private View loading, retry;
    private int requestVersion;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_mis_taxis);
        ((TextView)findViewById(R.id.tv_title)).setText(R.string.mis_taxis_title);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        list = findViewById(R.id.taxi_list_cards);
        status = findViewById(R.id.taxi_list_status);
        loading = findViewById(R.id.taxi_list_loading);
        retry = findViewById(R.id.taxi_list_retry);
        retry.setOnClickListener(v -> load());
    }

    @Override protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        int version = ++requestVersion;
        render(Collections.emptyList(), true, null);
        TaxiTasks.run(this, () -> ClientTaxiRepository.listar(this),
                rows -> { if (version == requestVersion) render(rows, false, null); },
                error -> { if (version == requestVersion) render(Collections.emptyList(), false, error); });
    }

    private void render(List<JSONObject> rows, boolean busy, String error) {
        list.removeAllViews();
        loading.setVisibility(busy ? View.VISIBLE : View.GONE);
        retry.setVisibility(error != null ? View.VISIBLE : View.GONE);
        status.setVisibility(busy || error != null || rows.isEmpty() ? View.VISIBLE : View.GONE);
        status.setText(busy ? getString(R.string.mis_taxis_loading)
                : error != null ? error : getString(R.string.mis_taxis_empty));
        for (JSONObject row : rows) addCard(row);
    }

    private void addCard(JSONObject row) {
        View card = getLayoutInflater().inflate(R.layout.item_solicitud_taxi, list, false);
        String state = row.optString("estado");
        boolean finished = "FINALIZADO".equals(state);
        TextView badge = card.findViewById(R.id.taxi_card_state);
        try {
            badge.setText(TaxiRepository.Estado.valueOf(state).texto);
        } catch (IllegalArgumentException e) {
            badge.setText("Estado por consultar");
        }
        boolean waiting = "SOLICITADO".equals(state);
        badge.setBackgroundTintList(ColorStateList.valueOf(getColor(waiting ? R.color.io_warning_bg
                : finished ? R.color.io_tag_bg : R.color.io_info_bg)));
        badge.setTextColor(getColor(waiting ? R.color.io_warning_text : R.color.io_navy));
        ((TextView)card.findViewById(R.id.taxi_card_reference)).setText(
                getString(R.string.mis_taxis_reference, row.optInt("reserva"), row.optInt("id")));
        ((TextView)card.findViewById(R.id.taxi_card_hotel)).setText(value(row, "hotel"));
        ((TextView)card.findViewById(R.id.taxi_card_destination)).setText(value(row, "destino"));
        ((TextView)card.findViewById(R.id.taxi_card_pickup)).setText(getString(R.string.mis_taxis_pickup, value(row, "hora")));
        TextView driver = card.findViewById(R.id.taxi_card_driver);
        String name = row.optString("conductor");
        driver.setVisibility(name.isEmpty() && !waiting ? View.GONE : View.VISIBLE);
        driver.setText(name.isEmpty() ? getString(R.string.mis_taxis_waiting) : getString(R.string.mis_taxis_driver, name));
        ((TextView)card.findViewById(R.id.taxi_card_action)).setText(
                finished ? R.string.mis_taxis_history : R.string.mis_taxis_tracking);
        card.setOnClickListener(v -> startActivity(new Intent(this, TaxistaAsignadoActivity.class)
                .putExtra(TaxiRepository.EXTRA_ID, row.optInt("id"))));
        list.addView(card);
    }

    private String value(JSONObject row, String key) {
        String value = row.optString(key, "").trim();
        return value.isEmpty() ? getString(R.string.mis_taxis_unspecified) : value;
    }
}
