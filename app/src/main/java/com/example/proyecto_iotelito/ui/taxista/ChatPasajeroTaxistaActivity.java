package com.example.proyecto_iotelito.ui.taxista;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.TaxiRepository;
import com.example.proyecto_iotelito.data.taxiapi.TaxiApi;
import com.example.proyecto_iotelito.data.taxiapi.TaxiApis;
import com.example.proyecto_iotelito.data.taxiapi.TaxiTasks;
import com.google.firebase.auth.FirebaseAuth;
import java.util.UUID;
import org.json.JSONArray;
import org.json.JSONObject;

/** Conversación del viaje abierta desde el seguimiento del cliente o el servicio del taxista. */
public class ChatPasajeroTaxistaActivity extends AppCompatActivity {

    public static final String EXTRA_NOMBRE_PASAJERO = "extra_nombre_pasajero";
    public static final String EXTRA_INICIALES_PASAJERO = "extra_iniciales_pasajero";

    public static Intent createIntent(Context context, String nombrePasajero, String iniciales) {
        Intent intent = new Intent(context, ChatPasajeroTaxistaActivity.class);
        intent.putExtra(EXTRA_NOMBRE_PASAJERO, nombrePasajero);
        intent.putExtra(EXTRA_INICIALES_PASAJERO, iniciales);
        return intent;
    }

    private TaxistaViewModel vm;
    private int servicio;
    private LinearLayout mensajesContainer;
    private ScrollView scrollMensajes;
    private TaxiApi api;
    private EditText entrada;
    private View enviar;
    private TextView estado;
    private long ultimoMensaje = -1;
    private boolean permiteEnviar, enviando, finalizado;
    private String textoPendiente, uuidPendiente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean remoto = FirebaseAuth.getInstance().getCurrentUser() != null;
        if (!remoto && !TaxistaFlujo.sesion(this)) return;
        setContentView(R.layout.activity_chat_pasajero_taxista);
        servicio = getIntent().getIntExtra(TaxiRepository.EXTRA_ID,
                getIntent().getIntExtra(TaxistaFlujo.ID, 0));

        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE_PASAJERO);
        if (TextUtils.isEmpty(nombre)) {
            nombre = getString(R.string.taxi_chat_listo);
        }
        String iniciales = getIntent().getStringExtra(EXTRA_INICIALES_PASAJERO);
        if (TextUtils.isEmpty(iniciales)) {
            iniciales = nombre.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        }

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_pasajero_nombre)).setText(nombre);
        ((TextView) findViewById(R.id.tv_avatar_inicial)).setText(iniciales);

        mensajesContainer = findViewById(R.id.mensajes_container);
        scrollMensajes = findViewById(R.id.scroll_mensajes);

        estado = findViewById(R.id.tv_taxi_chat_presence);
        entrada = findViewById(R.id.et_mensaje);
        enviar = findViewById(R.id.btn_enviar);
        entrada.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                enviarMensaje();
                return true;
            }
            return false;
        });
        enviar.setOnClickListener(v -> enviarMensaje());

        if (remoto) {
            iniciarChatRemoto();
        } else {
            estado.setText(R.string.taxi_chat_listo);
            vm = TaxistaFlujo.model(this);
            vm.repo.mensajes(servicio).observe(this, rows -> {
                mensajesContainer.removeAllViews();
                agregarBurbuja(getString(R.string.taxista_local_chat_demo), false);
                for (var mensaje : rows) agregarBurbuja(mensaje.texto, mensaje.mio);
                scrollAlFinal();
            });
            vm.repo.error.observe(this, error -> {
                if (error != null) enviar.setEnabled(true);
            });
        }
    }

    private void iniciarChatRemoto() {
        habilitarEntrada();
        if (servicio <= 0) {
            estado.setText(R.string.taxi_chat_sin_identificador);
            return;
        }
        try {
            api = TaxiApis.get(this);
        } catch (RuntimeException error) {
            estado.setText(TaxiTasks.message(error));
            return;
        }
        TaxiTasks.poll(this, 3000, () -> api.mensajes(servicio), this::mostrarConversacion, estado::setText);
    }

    private void mostrarConversacion(JSONObject chat) {
        boolean activo = chat.optBoolean("permiteEnviar");
        finalizado |= !activo;
        permiteEnviar = !finalizado;
        estado.setText(permiteEnviar ? R.string.taxi_chat_listo : R.string.taxi_chat_finalizado);
        habilitarEntrada();
        JSONArray mensajes = chat.optJSONArray("mensajes");
        if (mensajes == null) return;
        JSONObject ultimo = mensajes.optJSONObject(mensajes.length() - 1);
        long nuevoId = ultimo == null ? 0 : ultimo.optLong("id");
        if (nuevoId <= ultimoMensaje) return;
        ultimoMensaje = nuevoId;
        mensajesContainer.removeAllViews();
        for (int i = 0; i < mensajes.length(); i++) {
            JSONObject mensaje = mensajes.optJSONObject(i);
            if (mensaje != null) agregarBurbuja(mensaje.optString("texto"), mensaje.optBoolean("mio"));
        }
        scrollAlFinal();
    }

    private void habilitarEntrada() {
        entrada.setEnabled(permiteEnviar && !enviando);
        enviar.setEnabled(permiteEnviar && !enviando);
    }

    private void enviarMensaje() {
        String texto = entrada.getText().toString().trim();
        if (texto.isEmpty() || !enviar.isEnabled()) return;
        if (api != null) {
            if (!texto.equals(textoPendiente)) {
                textoPendiente = texto;
                uuidPendiente = UUID.randomUUID().toString();
            }
            String uuid = uuidPendiente;
            enviando = true;
            habilitarEntrada();
            TaxiTasks.run(this, () -> api.enviarMensaje(servicio, uuid, texto), chat -> {
                enviando = false;
                entrada.setText("");
                textoPendiente = null;
                mostrarConversacion(chat);
            }, error -> {
                enviando = false;
                habilitarEntrada();
                estado.setText(error);
                android.widget.Toast.makeText(this, error, android.widget.Toast.LENGTH_SHORT).show();
            });
            return;
        }
        enviar.setEnabled(false);
        vm.repo.ejecutar(this, () -> {
            vm.repo.mensaje(servicio, texto, true);
            vm.repo.mensaje(servicio, getString(R.string.taxi_respuesta_automatica_pasajero), false);
            return true;
        }, ok -> {
            entrada.setText("");
            enviar.setEnabled(true);
        });
    }

    private void agregarBurbuja(String texto, boolean esMia) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams filaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        filaParams.topMargin = dp(6);
        fila.setLayoutParams(filaParams);
        fila.setGravity(esMia ? Gravity.END : Gravity.START);

        TextView burbuja = new TextView(this);
        burbuja.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        burbuja.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.72f));
        burbuja.setPadding(dp(12), dp(8), dp(12), dp(8));
        burbuja.setText(texto);
        burbuja.setTextSize(13.5f);
        burbuja.setBackgroundResource(esMia ? R.drawable.bg_bubble_derecha : R.drawable.bg_bubble_izquierda);
        burbuja.setTextColor(ContextCompat.getColor(this,
                esMia ? R.color.white : R.color.io_text_primary));

        fila.addView(burbuja);
        mensajesContainer.addView(fila);
    }

    private void scrollAlFinal() {
        scrollMensajes.post(() -> scrollMensajes.fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
