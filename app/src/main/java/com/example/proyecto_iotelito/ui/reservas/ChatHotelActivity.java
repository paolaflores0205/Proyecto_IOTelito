package com.example.proyecto_iotelito.ui.reservas;

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
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Mensaje;

import java.util.List;

/**
 * Chat estático con el hotel (sin backend). Los mensajes que el huésped
 * envía se agregan localmente y reciben una respuesta automática tras un
 * breve retraso, para simular una conversación real (Clase 3.2 - UI).
 */
public class ChatHotelActivity extends AppCompatActivity {

    public static final String EXTRA_HOTEL_ID = "extra_hotel_id";
    public static final String EXTRA_CONVERSACION_ID = "extra_conversacion_id";

    private LinearLayout mensajesContainer;
    private ScrollView scrollMensajes;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_hotel);

        int hotelId = getIntent().getIntExtra(EXTRA_HOTEL_ID, 1);
        int conversacionId = getIntent().getIntExtra(EXTRA_CONVERSACION_ID, 0);
        Hotel hotel = SampleData.findById(hotelId);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) findViewById(R.id.tv_hotel_nombre)).setText(hotel.name);
        ((TextView) findViewById(R.id.tv_avatar_inicial)).setText(hotel.name.substring(0, 1));

        mensajesContainer = findViewById(R.id.mensajes_container);
        scrollMensajes = findViewById(R.id.scroll_mensajes);

        List<Mensaje> historial = SampleData.chatDeConversacion(conversacionId, hotel.name);
        for (Mensaje mensaje : historial) {
            agregarBurbuja(mensaje);
        }
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
        agregarBurbuja(new Mensaje(texto, true, "ahora"));
        etMensaje.setText("");
        scrollAlFinal();

        handler.postDelayed(() -> {
            agregarBurbuja(new Mensaje(getString(R.string.respuesta_automatica), false, "ahora"));
            scrollAlFinal();
        }, 1200);
    }

    private void agregarBurbuja(Mensaje mensaje) {
        LinearLayout fila = new LinearLayout(this);
        fila.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams filaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        filaParams.topMargin = dp(6);
        fila.setLayoutParams(filaParams);
        fila.setGravity(mensaje.deHuesped ? Gravity.END : Gravity.START);

        TextView burbuja = new TextView(this);
        burbuja.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        burbuja.setMaxWidth((int) (getResources().getDisplayMetrics().widthPixels * 0.72f));
        burbuja.setPadding(dp(12), dp(8), dp(12), dp(8));
        burbuja.setText(mensaje.texto);
        burbuja.setTextSize(13.5f);
        burbuja.setBackgroundResource(mensaje.deHuesped ? R.drawable.bg_bubble_derecha : R.drawable.bg_bubble_izquierda);
        burbuja.setTextColor(ContextCompat.getColor(this,
                mensaje.deHuesped ? R.color.white : R.color.io_text_primary));

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
