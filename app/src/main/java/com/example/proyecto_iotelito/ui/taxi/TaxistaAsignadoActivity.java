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
        ((TextView) findViewById(R.id.tv_detalle_taxi_cliente)).setText(getString(R.string.taxi_estado_detalle,
                pedido.optString("hotel"), pedido.optString("conductor"), pedido.optString("hora"),
                pedido.optInt("pasajeros"), pedido.optInt("maletas")));
    }
}
