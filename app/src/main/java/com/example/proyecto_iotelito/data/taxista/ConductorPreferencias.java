package com.example.proyecto_iotelito.data.taxista;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;
public final class ConductorPreferencias {
    private static final java.util.Set<String> VERIFICADAS=java.util.concurrent.ConcurrentHashMap.newKeySet();
    static void confirmar(String cuenta,boolean confirmada) {
        String key=cuenta.toLowerCase(Locale.ROOT);
        if(confirmada)VERIFICADAS.add(key);else VERIFICADAS.remove(key);
    }
    public static SharedPreferences get(Context c,String cuenta) {
        return c.getSharedPreferences("taxista_"+cuenta.toLowerCase(Locale.ROOT),Context.MODE_PRIVATE);
    }
    public static boolean disponible(Context c,String cuenta) {
        com.google.firebase.auth.FirebaseUser user=com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        boolean remoto=user!=null&&cuenta.equalsIgnoreCase(user.getEmail())
                &&TaxistaAuthRepository.restaurar(c,user)!=null;
        SharedPreferences prefs=get(c,cuenta);
        return (!remoto||(VERIFICADAS.contains(cuenta.toLowerCase(Locale.ROOT))
                &&prefs.getBoolean("disponible_confirmada",false)))
                &&prefs.getBoolean("disponible",!remoto);
    }
    private ConductorPreferencias() { }
}
