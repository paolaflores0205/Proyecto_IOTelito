package com.example.proyecto_iotelito.ui.taxi;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.model.auth.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;

/** Conversación compartida del cliente y el conductor asignado; no genera respuestas automáticas. */
public class TaxiChatActivity extends AppCompatActivity {
    private ListenerRegistration serviceListener,messagesListener;
    private LinearLayout messages;private EditText input;private TextView status;
    private String uid,role;private boolean allowed,sending;
    private DocumentReference service;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setContentView(R.layout.activity_chat_pasajero_taxista);
        AuthenticatedUser user=SessionRepository.obtener(this);
        if(user==null||user.getUid()==null||(user.getRole()!=UserRole.CLIENT&&user.getRole()!=UserRole.TAXISTA)){finish();return;}
        uid=user.getUid();role=user.getRole().name();
        int id=getIntent().getIntExtra(TaxiRepository.EXTRA_ID,-1);if(id<=0){finish();return;}
        service=FirebaseFirestore.getInstance().collection("taxiRequests").document(String.valueOf(id));
        messages=findViewById(R.id.mensajes_container);input=findViewById(R.id.et_mensaje);input.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4000)});
        findViewById(R.id.iv_back).setOnClickListener(v->finish());
        ((TextView)findViewById(R.id.tv_pasajero_nombre)).setText("Chat del servicio #"+id);
        ((TextView)findViewById(R.id.tv_avatar_inicial)).setText("T");
        ((TextView)findViewById(R.id.tv_taxi_chat_presence)).setText("Conversación del traslado");
        status=new TextView(this);status.setPadding(16,12,16,12);status.setText("Conectando con el servicio…");
        ((ViewGroup)messages.getParent().getParent()).addView(status,1);
        findViewById(R.id.btn_enviar).setEnabled(false);
        findViewById(R.id.btn_enviar).setOnClickListener(v->send());
        serviceListener=service.addSnapshotListener((doc,error)->{
            if(!session()||error!=null||doc==null||!doc.exists()){allowed=false;status.setText("No se pudo consultar el servicio. Revisa la conexión y el acceso.");update();return;}
            String driver=doc.getString("taxistaUid"),stateName=doc.getString("estado");
            boolean participant=uid.equals(doc.getString("clientUid"))||uid.equals(driver);
            allowed=participant&&TaxiChatPolicy.permiteEnviar(driver,stateName);
            status.setText(!participant?"No tienes acceso a este chat.":!TaxiChatPolicy.asignado(driver,stateName)?"El chat se habilita cuando un taxista acepta la solicitud.":"FINALIZADO".equals(stateName)?"Servicio finalizado. Historial de solo lectura.":"Chat conectado con "+("CLIENT".equals(role)?doc.getString("conductor"):doc.getString("nombre")));
            update();
        });
        messagesListener=service.collection("messages").orderBy("createdAt").limitToLast(200).addSnapshotListener((rows,error)->{
            if(!session())return;
            if(error!=null){status.setText("No se pudo cargar el chat. Revisa la conexión y las reglas de Firestore.");return;}
            messages.removeAllViews();
            if(rows!=null)for(DocumentSnapshot row:rows){
                TextView bubble=new TextView(this);boolean mine=uid.equals(row.getString("senderUid"));
                LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-2,-2);params.gravity=mine?Gravity.END:Gravity.START;params.topMargin=12;
                bubble.setLayoutParams(params);bubble.setPadding(24,16,24,16);bubble.setTextSize(15);
                String date=row.getTimestamp("createdAt")==null?"Enviando…":java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(row.getTimestamp("createdAt").toDate());
                bubble.setText(row.getString("text")+"\n"+date);bubble.setBackgroundResource(mine?R.drawable.bg_bubble_derecha:R.drawable.bg_bubble_izquierda);
                bubble.setTextColor(getColor(mine?R.color.white:R.color.io_text_primary));messages.addView(bubble);
            }
            ScrollView scroll=findViewById(R.id.scroll_mensajes);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));
        });
    }
    private boolean session(){return FirebaseAuth.getInstance().getCurrentUser()!=null&&uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid());}
    private void update(){findViewById(R.id.btn_enviar).setEnabled(allowed&&!sending);input.setEnabled(allowed&&!sending);}
    private void send(){
        if(!allowed||sending||!session())return;
        if(!ConexionCliente.disponible(this)){Toast.makeText(this,R.string.cliente_sin_conexion,Toast.LENGTH_LONG).show();return;}
        final String text;try{text=TaxiChatPolicy.validar(input.getText().toString());}catch(IllegalArgumentException error){input.setError(error.getMessage());return;}
        Map<String,Object> data=new HashMap<>();data.put("senderUid",uid);data.put("senderRole",role);data.put("text",text);data.put("createdAt",FieldValue.serverTimestamp());
        sending=true;update();service.collection("messages").document().set(data).addOnSuccessListener(v->{sending=false;if(isDestroyed())return;input.setText("");update();})
            .addOnFailureListener(error->{sending=false;if(isDestroyed())return;update();Toast.makeText(this,"No se pudo enviar. Conservamos tu mensaje para reintentar.",Toast.LENGTH_LONG).show();});
    }
    @Override protected void onDestroy(){if(serviceListener!=null)serviceListener.remove();if(messagesListener!=null)messagesListener.remove();super.onDestroy();}
}
