package com.example.proyecto_iotelito.ui.taxi;

import android.os.Bundle;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.model.auth.*;

/** No consulta taxiRequests con IDs de la nueva API: podrían pertenecer a viajes antiguos. */
public class TaxiChatActivity extends AppCompatActivity {
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        AuthenticatedUser user=SessionRepository.obtener(this);
        if(user==null || (user.getRole()!=UserRole.CLIENT && user.getRole()!=UserRole.TAXISTA)) { finish(); return; }
        setContentView(R.layout.activity_chat_pasajero_taxista);
        findViewById(R.id.iv_back).setOnClickListener(v->finish());
        ((TextView)findViewById(R.id.tv_pasajero_nombre)).setText("Chat del traslado");
        ((TextView)findViewById(R.id.tv_taxi_chat_presence)).setText("Mensajería no disponible para este traslado");
        TextView notice=new TextView(this);
        notice.setPadding(24,24,24,24);
        notice.setText("El servicio de taxis todavía no ofrece mensajería. Coordina el recojo con la recepción del hotel. El seguimiento del viaje sigue disponible.");
        ((LinearLayout)findViewById(R.id.mensajes_container)).addView(notice);
        ((EditText)findViewById(R.id.et_mensaje)).setEnabled(false);
        findViewById(R.id.btn_enviar).setEnabled(false);
    }
}
