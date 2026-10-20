package com.example.proyecto_iotelito.data.taxista;
import android.content.Context;
import android.content.SharedPreferences;
import java.util.Locale;
public final class ConductorPreferencias {
    public static SharedPreferences get(Context c,String cuenta) {
        return c.getSharedPreferences("taxista_"+cuenta.toLowerCase(Locale.ROOT),Context.MODE_PRIVATE);
    }
    public static boolean disponible(Context c,String cuenta) { return get(c,cuenta).getBoolean("disponible",true); }
    private ConductorPreferencias() { }
}
