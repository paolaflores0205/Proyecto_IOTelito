package com.example.proyecto_iotelito.notification;

import android.app.Activity;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.data.taxiapi.TaxiTasks;
import com.example.proyecto_iotelito.data.taxista.TaxistaAuthRepository;
import com.example.proyecto_iotelito.model.auth.*;
import com.example.proyecto_iotelito.ui.auth.LoginActivity;
import com.example.proyecto_iotelito.ui.reservas.DetalleReservaActivity;
import com.example.proyecto_iotelito.ui.taxi.TaxistaAsignadoActivity;
import com.example.proyecto_iotelito.ui.taxista.TaxistaMainActivity;
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

    public static Intent paraTaxista(Context c, Intent destino, String uid) {
        String binding = ClientPushRegistration.bindingTaxista(c, uid);
        if (binding == null) return destino;
        return new Intent(c, ClientNotificationActivity.class).putExtras(destino)
                .putExtra("ownerUid", uid).putExtra("driverBinding", binding);
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        String uid=getIntent().getStringExtra("ownerUid");
        String binding = getIntent().getStringExtra("driverBinding");
        if (binding != null) {
            abrirComoTaxista(uid, binding);
            return;
        }
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

    private void abrirComoTaxista(String uid, String binding) {
        if (!binding.equals(ClientPushRegistration.bindingTaxista(this, uid))) {
            login();
            return;
        }
        TaxistaAuthRepository.resolver().addOnSuccessListener(this, user -> {
            if (isFinishing()) return;
            if (!binding.equals(ClientPushRegistration.bindingTaxista(this, uid))) {
                login();
                return;
            }
            SessionRepository.guardar(this, user);
            Intent destino = new Intent(this, TaxistaMainActivity.class).putExtras(getIntent())
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(destino);
            finish();
        }).addOnFailureListener(this, error -> {
            android.widget.Toast.makeText(this, TaxiTasks.message(error), android.widget.Toast.LENGTH_LONG).show();
            finish();
        });
    }
    private void login(){startActivity(new Intent(this,LoginActivity.class));finish();}
}
