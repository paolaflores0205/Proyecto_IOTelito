package com.example.proyecto_iotelito.ui.taxista;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.data.TaxiRepository;
import com.example.proyecto_iotelito.ui.taxi.TaxiChatActivity;

/** El chat del traslado necesita un contrato de backend; nunca genera respuestas ficticias. */
public class ChatPasajeroTaxistaActivity extends AppCompatActivity {
    public static final String EXTRA_NOMBRE_PASAJERO="extra_nombre_pasajero";
    public static final String EXTRA_INICIALES_PASAJERO="extra_iniciales_pasajero";
    public static Intent createIntent(Context context,String name,String initials) {
        return new Intent(context,ChatPasajeroTaxistaActivity.class)
                .putExtra(EXTRA_NOMBRE_PASAJERO,name).putExtra(EXTRA_INICIALES_PASAJERO,initials);
    }
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        if(!TaxistaFlujo.sesion(this)) return;
        startActivity(new Intent(this,TaxiChatActivity.class)
                .putExtra(TaxiRepository.EXTRA_ID,getIntent().getIntExtra(TaxistaFlujo.ID,-1)));
        finish();
    }
}
