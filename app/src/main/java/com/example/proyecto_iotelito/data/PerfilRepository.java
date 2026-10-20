package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.proyecto_iotelito.model.UserProfile;

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

    public static UserProfile cargar(Context context) {
        UserProfile valoresIniciales = SampleData.PERFIL;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
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
