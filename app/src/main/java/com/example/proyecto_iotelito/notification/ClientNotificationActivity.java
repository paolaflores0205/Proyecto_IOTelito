package com.example.proyecto_iotelito.notification;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.model.auth.*;
import com.example.proyecto_iotelito.ui.auth.LoginActivity;
import com.example.proyecto_iotelito.ui.reservas.DetalleReservaActivity;
import com.example.proyecto_iotelito.ui.taxi.TaxistaAsignadoActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

/** Restaura el rol desde servidor al abrir un aviso con el proceso antes cerrado. */
public class ClientNotificationActivity extends Activity {
    public static Intent intent(Context c,String uid,int notice,int reservation,int taxi,boolean checkout){
        return new Intent(c,ClientNotificationActivity.class)
            .setData(new Uri.Builder().scheme("iotelito").authority("client-notice").appendPath(uid).appendPath(String.valueOf(notice)).build())
            .putExtra("ownerUid",uid).putExtra("noticeId",notice).putExtra("reservationId",reservation)
            .putExtra("taxiId",taxi).putExtra("checkout",checkout);
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        String uid=getIntent().getStringExtra("ownerUid");
        if(FirebaseAuth.getInstance().getCurrentUser()==null||!FirebaseAuth.getInstance().getCurrentUser().getUid().equals(uid)){login();return;}
        FirebaseFirestore.getInstance().collection("users").document(uid).get(Source.SERVER).addOnSuccessListener(doc->{
            if(isFinishing())return;
            try{
                if(FirebaseAuth.getInstance().getCurrentUser()==null||!uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())){login();return;}
                AuthenticatedUser user=RemoteUserProfile.parse(uid,doc.getData()).authenticated();
                if(user.getRole()!=UserRole.CLIENT){login();return;}
                SessionRepository.guardar(this,user);
                int reservation=getIntent().getIntExtra("reservationId",-1),taxi=getIntent().getIntExtra("taxiId",-1);
                if(reservation<=0){finish();return;}
                if(getIntent().getBooleanExtra("checkout",false))ClientCheckoutAlerts.markRead(this,reservation);
                else NotificacionRepository.getInstance(this).marcarLeida(getIntent().getIntExtra("noticeId",-1));
                Intent target=taxi>0?new Intent(this,TaxistaAsignadoActivity.class).putExtra(TaxiRepository.EXTRA_ID,taxi)
                    :new Intent(this,DetalleReservaActivity.class).putExtra(DetalleReservaActivity.EXTRA_RESERVA_ID,reservation);
                startActivity(target);finish();
            }catch(IllegalArgumentException e){login();}
        }).addOnFailureListener(e->{android.widget.Toast.makeText(this,"No se pudo abrir el aviso. Revisa tu conexión y reintenta.",android.widget.Toast.LENGTH_LONG).show();finish();});
    }
    private void login(){startActivity(new Intent(this,LoginActivity.class));finish();}
}
