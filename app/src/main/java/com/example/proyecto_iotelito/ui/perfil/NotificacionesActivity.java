package com.example.proyecto_iotelito.ui.perfil;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.NotificacionRepository;
import com.example.proyecto_iotelito.data.TaxiRepository;
import com.example.proyecto_iotelito.model.NotificacionCliente;
import com.example.proyecto_iotelito.ui.reservas.DetalleReservaActivity;

import java.util.List;

/**
 * Bandeja persistente de las notificaciones generadas por reservas y pagos.
 */
public class NotificacionesActivity extends AppCompatActivity {

    private NotificacionRepository repository;
    private NotificacionAdapter adapter;
    private View vacio;
    private View marcarLeidas;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notificaciones);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        repository = NotificacionRepository.getInstance(this);

        RecyclerView recyclerView = findViewById(R.id.rv_notificaciones);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NotificacionAdapter(this::abrirNotificacion);
        recyclerView.setAdapter(adapter);
        vacio = findViewById(R.id.container_sin_notificaciones);
        marcarLeidas = findViewById(R.id.tv_marcar_leidas);
        marcarLeidas.setOnClickListener(v -> {
            repository.marcarTodasLeidas();
        });
        repository.observarTodas().observe(this, this::cargarNotificaciones);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void cargarNotificaciones(List<NotificacionCliente> notificaciones) {
        adapter.submitList(notificaciones);
        boolean sinNotificaciones = notificaciones.isEmpty();
        boolean hayNoLeidas = false;
        for (NotificacionCliente notificacion : notificaciones) {
            if (!notificacion.leida) {
                hayNoLeidas = true;
                break;
            }
        }
        vacio.setVisibility(sinNotificaciones ? View.VISIBLE : View.GONE);
        marcarLeidas.setVisibility(hayNoLeidas ? View.VISIBLE : View.GONE);
        findViewById(R.id.rv_notificaciones).setVisibility(
                sinNotificaciones ? View.GONE : View.VISIBLE);
    }

    private void abrirNotificacion(NotificacionCliente notificacion) {
        repository.marcarLeida(notificacion.id);
        if (notificacion.tipo == NotificacionCliente.Tipo.TAXI) {
            TaxiRepository.ejecutar(this, this, () -> TaxiRepository.porReserva(this, notificacion.reservaId), id -> {
                if (id >= 0 && !isFinishing()) startActivity(
                        new Intent(this, com.example.proyecto_iotelito.ui.taxi.TaxistaAsignadoActivity.class)
                                .putExtra(TaxiRepository.EXTRA_ID, id));
            });
            return;
        }
        Intent intent = new Intent(this, DetalleReservaActivity.class);
        intent.putExtra(DetalleReservaActivity.EXTRA_RESERVA_ID, notificacion.reservaId);
        intent.putExtra(DetalleReservaActivity.EXTRA_NOTIFICACION_ID, notificacion.id);
        startActivity(intent);
    }
}
