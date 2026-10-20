package com.example.proyecto_iotelito.ui.taxi;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.contract.ActivityResultContracts;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.MainActivity;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.data.TaxiRepository;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.notification.ClienteNotificationManager;
import org.json.JSONObject;

public class TaxistaAsignadoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_estado_taxi_cliente);
        android.view.View header = findViewById(R.id.sub_header);
        ((TextView) header.findViewById(R.id.tv_title)).setText(R.string.taxi_seguimiento_titulo);
        header.findViewById(R.id.iv_back).setOnClickListener(view -> volverAlInicio());
        findViewById(R.id.btn_volver_taxi).setOnClickListener(view -> volverAlInicio());
        androidx.activity.result.ActivityResultLauncher<String> permiso = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), granted -> {
                    if (granted) publicarEstadoActual();
                    else android.widget.Toast.makeText(this, R.string.taxi_permiso_notificaciones,
                            android.widget.Toast.LENGTH_LONG).show();
                });
        if (!ClienteNotificationManager.tienePermiso(this)) permiso.launch(Manifest.permission.POST_NOTIFICATIONS);
        int id = getIntent().getIntExtra(TaxiRepository.EXTRA_ID, -1);
        TaxiRepository.ejecutar(this, this, () -> TaxiRepository.buscar(this, id), pedido ->
                TaxiRepository.observar(this, id).observe(this, this::pintar));
    }

    private void publicarEstadoActual() {
        int id = getIntent().getIntExtra(TaxiRepository.EXTRA_ID, -1);
        TaxiRepository.ejecutar(this, this, () -> {
            JSONObject pedido = TaxiRepository.buscar(this, id);
            AuthenticatedUser user = SessionRepository.obtener(this);
            if (pedido != null && user != null && user.getEmail().equals(pedido.optString("cliente"))) {
                ClienteNotificationManager.mostrarEstadoTaxi(this, id, pedido.optInt("reserva"),
                        TaxiRepository.Estado.valueOf(pedido.optString("estado")));
            }
            return true;
        }, enviado -> { });
    }

    private void volverAlInicio() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, R.id.nav_explorar);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void pintar(JSONObject pedido) {
        AuthenticatedUser user = SessionRepository.obtener(this);
        if (user == null) {
            startActivity(new android.content.Intent(this, com.example.proyecto_iotelito.ui.auth.LoginActivity.class)
                    .putExtra(TaxiRepository.EXTRA_ID, getIntent().getIntExtra(TaxiRepository.EXTRA_ID, -1)));
            finish();
            return;
        }
        if (pedido == null || !user.getEmail().equals(pedido.optString("cliente"))) {
            finish();
            return;
        }
        TaxiRepository.Estado estado = TaxiRepository.Estado.valueOf(pedido.optString("estado"));
        ((TextView) findViewById(R.id.tv_estado_taxi_cliente)).setText(estado.texto);
        int[] titulares = {R.string.taxi_hero_solicitado, R.string.taxi_hero_asignado,
                R.string.taxi_hero_camino, R.string.taxi_hero_finalizado};
        int[] descripciones = {R.string.taxi_desc_solicitado, R.string.taxi_desc_asignado,
                R.string.taxi_desc_camino, R.string.taxi_desc_finalizado};
        ((TextView) findViewById(R.id.tv_taxi_titular)).setText(titulares[estado.ordinal()]);
        ((TextView) findViewById(R.id.tv_taxi_descripcion)).setText(descripciones[estado.ordinal()]);
        ((TextView) findViewById(R.id.tv_taxi_folio)).setText(getString(R.string.taxi_seguimiento_folio, pedido.optInt("id")));
        ((TextView) findViewById(R.id.tv_taxi_hotel)).setText(pedido.optString("hotel"));
        ((TextView) findViewById(R.id.tv_taxi_origen)).setText(pedido.optString("origen", pedido.optString("hotel")));
        ((TextView) findViewById(R.id.tv_taxi_destino)).setText(pedido.optString("destino"));
        ((TextView) findViewById(R.id.tv_taxi_hora)).setText(pedido.optString("hora"));
        ((TextView) findViewById(R.id.tv_taxi_pasajeros)).setText(String.valueOf(pedido.optInt("pasajeros")));
        ((TextView) findViewById(R.id.tv_taxi_maletas)).setText(String.valueOf(pedido.optInt("maletas")));
        String conductor = pedido.optString("conductor");
        findViewById(R.id.card_taxi_conductor).setVisibility(conductor.isEmpty() ? android.view.View.GONE : android.view.View.VISIBLE);
        ((TextView) findViewById(R.id.tv_taxi_conductor)).setText(conductor);
        ((TextView) findViewById(R.id.tv_taxi_aviso)).setText(estado == TaxiRepository.Estado.FINALIZADO
                ? R.string.taxi_seguimiento_cerrado : R.string.taxi_seguimiento_aviso);
        int[] pasos = {R.id.tv_taxi_paso_solicitado, R.id.tv_taxi_paso_asignado,
                R.id.tv_taxi_paso_camino, R.id.tv_taxi_paso_finalizado};
        for (int indice = 0; indice < pasos.length; indice++) {
            TextView paso = findViewById(pasos[indice]);
            boolean alcanzado = indice <= estado.ordinal();
            int color = getColor(alcanzado ? R.color.io_teal : R.color.io_text_muted);
            paso.setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(color));
            paso.setTextColor(getColor(alcanzado ? R.color.io_navy : R.color.io_text_secondary));
            paso.setTypeface(null, indice == estado.ordinal() ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
            paso.setBackgroundResource(indice == estado.ordinal() ? R.drawable.bg_info_box_teal : 0);
            int descripcion = indice < estado.ordinal() ? R.string.taxi_paso_completo
                    : indice == estado.ordinal() ? R.string.taxi_paso_actual : R.string.taxi_paso_pendiente;
            paso.setContentDescription(getString(R.string.taxi_paso_accesibilidad, paso.getText(), getString(descripcion)));
        }
    }
}
