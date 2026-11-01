package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.proyecto_iotelito.model.UserProfile;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** Persiste los datos editables del perfil del cliente en SharedPreferences. */
public final class PerfilRepository {

    private static final String PREFS = "perfil_cliente";
    private static final String NOMBRE = "nombre";
    private static final String EMAIL = "email";
    private static final String TELEFONO = "telefono";
    private static final String TIPO_DOCUMENTO = "tipo_documento";
    private static final String NUMERO_DOCUMENTO = "numero_documento";
    private static final String FECHA_NACIMIENTO = "fecha_nacimiento";
    private static final String DOMICILIO = "domicilio";

    private PerfilRepository() { }

    private static SharedPreferences preferencias(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String fileName = user == null ? PREFS : PREFS + "_" + user.getUid();
        return context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
    }

    public static UserProfile cargar(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        UserProfile valoresIniciales = user == null ? SampleData.PERFIL
                : new UserProfile(user.getDisplayName() == null ? "" : user.getDisplayName(),
                user.getEmail() == null ? "" : user.getEmail(), "", "", "", "", "");
        SharedPreferences prefs = preferencias(context);
        return new UserProfile(
                prefs.getString(NOMBRE, valoresIniciales.name),
                prefs.getString(EMAIL, valoresIniciales.email),
                prefs.getString(TELEFONO, valoresIniciales.phone),
                prefs.getString(TIPO_DOCUMENTO, valoresIniciales.tipoDocumento),
                prefs.getString(NUMERO_DOCUMENTO, valoresIniciales.numeroDocumento),
                prefs.getString(FECHA_NACIMIENTO, valoresIniciales.fechaNacimiento),
                prefs.getString(DOMICILIO, valoresIniciales.domicilio));
    }

    public static void guardar(Context context, UserProfile perfil) {
        preferencias(context)
                .edit()
                .putString(NOMBRE, perfil.name)
                .putString(EMAIL, perfil.email)
                .putString(TELEFONO, perfil.phone)
                .putString(TIPO_DOCUMENTO, perfil.tipoDocumento)
                .putString(NUMERO_DOCUMENTO, perfil.numeroDocumento)
                .putString(FECHA_NACIMIENTO, perfil.fechaNacimiento)
                .putString(DOMICILIO, perfil.domicilio)
                .apply();
        aplicarEnMemoria(perfil);
    }

    /** Mantiene compatibles las pantallas que todavía consultan SampleData.PERFIL. */
    public static void aplicarEnMemoria(UserProfile perfil) {
        UserProfile actual = SampleData.PERFIL;
        actual.name = perfil.name;
        actual.email = perfil.email;
        actual.phone = perfil.phone;
        actual.tipoDocumento = perfil.tipoDocumento;
        actual.numeroDocumento = perfil.numeroDocumento;
        actual.fechaNacimiento = perfil.fechaNacimiento;
        actual.domicilio = perfil.domicilio;
    }
}
