package com.example.proyecto_iotelito.ui.taxista;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

/**
 * Chat estático con el pasajero (sin backend), mismo patrón que
 * {@link com.example.proyecto_iotelito.ui.reservas.ChatHotelActivity} del
 * cliente: los mensajes que el conductor envía se agregan localmente y
 * reciben una respuesta automática tras un breve retraso.
 */
public class ChatPasajeroTaxistaActivity extends AppCompatActivity {

    public static final String EXTRA_NOMBRE_PASAJERO = "extra_nombre_pasajero";
    public static final String EXTRA_INICIALES_PASAJERO = "extra_iniciales_pasajero";

    public static Intent createIntent(Context context, String nombrePasajero, String iniciales) {
        Intent intent = new Intent(context, ChatPasajeroTaxistaActivity.class);
        intent.putExtra(EXTRA_NOMBRE_PASAJERO, nombrePasajero);
        intent.putExtra(EXTRA_INICIALES_PASAJERO, iniciales);
        return intent;
    }

    private LinearLayout mensajesContainer;
    private ScrollView scrollMensajes;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_pasajero_taxista);

        String nombre = getIntent().getStringExtra(EXTRA_NOMBRE_PASAJERO);
        if (TextUtils.isEmpty(nombre)) {
            nombre = "Ana García";
        }
        String iniciales = getIntent().getStringExtra(EXTRA_INICIALES_PASAJERO);
        if (TextUtils.isEmpty(iniciales)) {
            iniciales = "AG";
        }

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_pasajero_nombre)).setText(nombre);
        ((TextView) findViewById(R.id.tv_avatar_inicial)).setText(iniciales);

        mensajesContainer = findViewById(R.id.mensajes_container);
        scrollMensajes = findViewById(R.id.scroll_mensajes);

        agregarBurbuja(getString(R.string.taxi_msg_pasajero_1), false);
        agregarBurbuja(getString(R.string.taxi_msg_taxista_1), true);
        scrollAlFinal();

        EditText etMensaje = findViewById(R.id.et_mensaje);
        etMensaje.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                enviarMensaje(etMensaje);
                return true;
            }
            return false;
        });
        findViewById(R.id.btn_enviar).setOnClickListener(v -> enviarMensaje(etMensaje));
    }

    private void enviarMensaje(EditText etMensaje) {
        String texto = etMensaje.getText().toString().trim();
        if (TextUtils.isEmpty(texto)) {
            return;
        }
        agregarBurbuja(texto, true);
        etMensaje.setText("");
        scrollAlFinal();

        handler.postDelayed(() -> {
            agregarBurbuja(getString(R.string.taxi_respuesta_automatica_pasajero), false);
            scrollAlFinal();
        }, 1200);
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
