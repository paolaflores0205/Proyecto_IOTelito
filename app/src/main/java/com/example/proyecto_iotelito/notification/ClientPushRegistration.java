package com.example.proyecto_iotelito.notification;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.model.auth.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.*;

/** Vincula esta instalación a una única sesión; un push antiguo nunca pasa a otra cuenta. */
public final class ClientPushRegistration {
    private ClientPushRegistration() { }
    private static volatile long generation;
    private static SharedPreferences prefs(Context c) { return c.getSharedPreferences("client_push_binding", Context.MODE_PRIVATE); }
    public static void register(Context context) {
        String project=com.google.firebase.FirebaseApp.getInstance().getOptions().getProjectId();
        if(project!=null && project.startsWith("demo-"))return; // FCM no tiene emulador local.
        long ticket=generation;
        Context app=context.getApplicationContext();
        AuthenticatedUser user=SessionRepository.obtener(app);
        if(user==null||user.getUid()==null||user.getRole()!=UserRole.CLIENT)return;
        String uid=user.getUid(); final String binding;
        synchronized(ClientPushRegistration.class) {
            if(ticket!=generation||FirebaseAuth.getInstance().getCurrentUser()==null||!uid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()))return;
            SharedPreferences p=prefs(app);
            if(!uid.equals(p.getString("uid",null))) p.edit().putString("uid",uid).putString("binding",UUID.randomUUID().toString()).commit();
            binding=p.getString("binding","");
        }
        FirebaseMessaging.getInstance().setAutoInitEnabled(true);
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(token -> upload(app,uid,binding,token))
                .addOnFailureListener(error -> android.util.Log.w("ClientPush","No se pudo registrar el dispositivo; se reintentará al abrir la app."));
    }
    public static void refresh(Context context,String token) {
        String uid=prefs(context).getString("uid",null),binding=prefs(context).getString("binding",null);
        if(matches(context,uid,binding))upload(context.getApplicationContext(),uid,binding,token);
    }
    private static void upload(Context app,String uid,String binding,String token) {
        if(!matches(app,uid,binding))return;
        Map<String,Object> data=new HashMap<>();
        data.put("token",token);data.put("binding",binding);data.put("platform","ANDROID");data.put("updatedAt",FieldValue.serverTimestamp());
        FirebaseFirestore.getInstance().collection("users").document(uid).collection("notificationDevices").document(binding)
            .set(data).addOnFailureListener(e -> android.util.Log.w("ClientPush","Registro pendiente; se reintentará al abrir la app."));
    }
    public static boolean isOwner(Context c,String uid) {
        return matches(c,uid,prefs(c).getString("binding",null));
    }
    public static boolean matches(Context c,String uid,String binding) {
        com.google.firebase.auth.FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();
        return ClientPushPolicy.accepts(uid,binding,user==null?null:user.getUid(),
                prefs(c).getString("uid",null),prefs(c).getString("binding",null));
    }
    public static synchronized void clear(Context c) {
        generation++;
        SharedPreferences p=prefs(c);String uid=p.getString("uid",null),binding=p.getString("binding",null);
        p.edit().clear().commit(); // Bloquear recepción inmediatamente, incluso si no hay internet.
        if(uid!=null&&binding!=null)FirebaseFirestore.getInstance().collection("users").document(uid)
            .collection("notificationDevices").document(binding).delete();
    }
}
