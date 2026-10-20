package com.example.proyecto_iotelito.ui.taxista;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.taxista.local.AvisoConductorEntity;

/** Clase 8.1: canal, permiso, Builder y PendingIntent con ID del servicio. */
public final class TaxistaNotificaciones {
    public static final String CHANNEL="taxista_servicios";
    public static void canal(Context c) {
        NotificationChannel canal=new NotificationChannel(CHANNEL,c.getString(R.string.taxista_local_canal),NotificationManager.IMPORTANCE_DEFAULT);
        canal.setDescription(c.getString(R.string.taxista_local_canal_desc));
        c.getSystemService(NotificationManager.class).createNotificationChannel(canal);
    }
    public static boolean permiso(Context c) {
        return ContextCompat.checkSelfPermission(c,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
    }
    public static boolean publicar(Context c,AvisoConductorEntity a) {
        canal(c);
        NotificationManager manager=c.getSystemService(NotificationManager.class);
        if(!permiso(c)||!NotificationManagerCompat.from(c).areNotificationsEnabled()
                ||manager.getNotificationChannel(CHANNEL).getImportance()==NotificationManager.IMPORTANCE_NONE) return false;
        Intent i=new Intent(c,TaxistaMainActivity.class).putExtra("taxista_aviso_cuenta",a.cuenta)
                .putExtra("taxista_aviso_id",a.servicio).putExtra("taxista_aviso_clave",a.clave)
                .putExtra("taxista_aviso_tipo",a.tipo).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent p=PendingIntent.getActivity(c,(a.cuenta+a.clave).hashCode(),i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        try {
            manager.notify("taxista:"+a.cuenta+":"+a.clave,0,new NotificationCompat.Builder(c,CHANNEL)
                    .setSmallIcon(R.drawable.ic_notifications).setContentTitle(a.titulo).setContentText(a.texto)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(a.texto)).setContentIntent(p).setAutoCancel(true).build());
            return true;
        } catch(SecurityException denied) { return false; }
    }
    public static void cancelar(Context c,String cuenta,int id) {
        c.getSystemService(NotificationManager.class).cancel("taxista:"+cuenta+":solicitud:"+id,0);
    }
}
