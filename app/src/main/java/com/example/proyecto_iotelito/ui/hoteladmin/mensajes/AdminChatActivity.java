package com.example.proyecto_iotelito.ui.hoteladmin.mensajes;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminChatBinding;
import com.example.proyecto_iotelito.model.Mensaje;
import com.example.proyecto_iotelito.model.hoteladmin.Conversacion;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;

/** Pantalla adm-chat-cliente. Los mensajes se conservan al rotar mediante Bundle. */
public class AdminChatActivity extends AppCompatActivity {
    public static final String EXTRA_CONVERSACION_ID = "conversacion_id";
    private static final String KEY_MENSAJES = "mensajes";

    private ActivityHoteladminChatBinding binding;
    private Conversacion conversacion;
    private ArrayList<Mensaje> mensajes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHoteladminChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        conversacion = HotelAdminSampleData.conversacion(
                getIntent().getIntExtra(EXTRA_CONVERSACION_ID, 1));
        if (savedInstanceState != null) {
            Object guardados = savedInstanceState.getSerializable(KEY_MENSAJES);
            mensajes = guardados instanceof ArrayList ? (ArrayList<Mensaje>) guardados : historialInicial();
        } else {
            mensajes = historialInicial();
        }

        binding.ivBack.setOnClickListener(v -> finish());
        binding.tvNombre.setText(conversacion.clienteNombre);
        binding.tvHabitacion.setText(conversacion.habitacion);
        binding.tvInicial.setText(conversacion.inicial);
        binding.tvVerReserva.setOnClickListener(v -> mostrarDetalleReserva());
        binding.btnAdjuntar.setOnClickListener(v -> Toast.makeText(
                this, R.string.hoteladmin_adjuntos_posterior, Toast.LENGTH_SHORT).show());
        binding.btnEnviar.setOnClickListener(v -> enviarMensaje());
        binding.etMensaje.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                enviarMensaje();
                return true;
            }
            return false;
        });
        renderMensajes();
    }

    private ArrayList<Mensaje> historialInicial() {
        return new ArrayList<>(HotelAdminSampleData.chatDe(conversacion.id));
    }

    private void enviarMensaje() {
        String texto = String.valueOf(binding.etMensaje.getText()).trim();
        if (TextUtils.isEmpty(texto)) {
            binding.etMensaje.setError(getString(R.string.hoteladmin_escribe_mensaje_error));
            return;
        }
        mensajes.add(new Mensaje(texto, false, getString(R.string.hoteladmin_hora_ahora)));
        binding.etMensaje.setText("");
        renderMensajes();
    }

    private void renderMensajes() {
        binding.llMensajes.removeAllViews();
        for (Mensaje mensaje : mensajes) agregarBurbuja(mensaje);
        binding.scrollMensajes.post(() -> binding.scrollMensajes.fullScroll(View.FOCUS_DOWN));
    }

    private void agregarBurbuja(Mensaje mensaje) {
        LinearLayout fila = new LinearLayout(this);
        fila.setGravity(mensaje.deHuesped ? Gravity.START : Gravity.END);
        LinearLayout.LayoutParams filaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        filaParams.topMargin = dp(8);
        fila.setLayoutParams(filaParams);

        LinearLayout bloque = new LinearLayout(this);
        bloque.setOrientation(LinearLayout.VERTICAL);
        bloque.setGravity(mensaje.deHuesped ? Gravity.START : Gravity.END);

        TextView burbuja = new TextView(this);
        burbuja.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.74f));
        burbuja.setPadding(dp(12), dp(9), dp(12), dp(9));
        burbuja.setText(mensaje.texto);
        burbuja.setTextSize(13.5f);
        burbuja.setBackgroundResource(mensaje.deHuesped
                ? R.drawable.bg_bubble_izquierda : R.drawable.bg_bubble_derecha);
        burbuja.setTextColor(ContextCompat.getColor(this,
                mensaje.deHuesped ? R.color.io_text_primary : R.color.white));

        TextView hora = new TextView(this);
        hora.setText(mensaje.hora);
        hora.setTextColor(ContextCompat.getColor(this, R.color.io_text_muted));
        hora.setTextSize(10f);
        hora.setTypeface(null, Typeface.NORMAL);
        hora.setPadding(dp(4), dp(2), dp(4), 0);
        bloque.addView(burbuja);
        bloque.addView(hora);
        fila.addView(bloque);
        binding.llMensajes.addView(fila);
    }

    private void mostrarDetalleReserva() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.hoteladmin_detalle_reserva_activa)
                .setMessage(getString(R.string.hoteladmin_detalle_reserva_chat,
                        conversacion.clienteNombre, conversacion.habitacion))
                .setPositiveButton(R.string.hoteladmin_entendido, null)
                .show();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_MENSAJES, mensajes);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
