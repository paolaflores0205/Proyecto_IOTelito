package com.example.proyecto_iotelito.data.taxista;

import android.content.Context;
import android.os.*;

import androidx.lifecycle.*;

import com.example.proyecto_iotelito.data.taxiapi.*;

import org.json.JSONObject;

import java.util.*;
import java.util.concurrent.*;

/** Estado del interruptor confirmado por la API, compartido por Inicio y Perfil. */
public final class DisponibilidadRepository implements AutoCloseable {
    private static final Map<String, DisponibilidadRepository> INSTANCIAS = new HashMap<>();
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private final Context context;
    private final String cuenta;
    private final TaxiApi api;
    private int usuarios;
    private volatile boolean cerrado;
    private boolean busy;
    private final MutableLiveData<DisponibilidadEstado> estado =
            new MutableLiveData<>(DisponibilidadEstado.inicial());
    public final MutableLiveData<String> error = new MutableLiveData<>();

    public static synchronized DisponibilidadRepository adquirir(Context c, String cuenta) {
        DisponibilidadRepository r = INSTANCIAS.get(cuenta);
        if (r == null) {
            r = new DisponibilidadRepository(c, cuenta);
            INSTANCIAS.put(cuenta, r);
        }
        r.usuarios++;
        return r;
    }

    private DisponibilidadRepository(Context c, String cuenta) {
        context = c.getApplicationContext();
        this.cuenta = cuenta;
        api = TaxiApis.get(c);
        refrescar();
    }

    public LiveData<DisponibilidadEstado> estado() {
        return estado;
    }

    public void refrescar() {
        operar(null);
    }

    public void cambiar(boolean value) {
        DisponibilidadEstado old = estado.getValue();
        if (old != null && old.editable() && old.disponible != value) operar(value);
    }

    private void operar(Boolean value) {
        if (cerrado || busy) return;
        busy = true;
        if (value != null) estado.setValue(estado.getValue().iniciarCambio());
        IO.execute(
                () -> {
                    try {
                        JSONObject p =
                                value == null
                                        ? api.miPerfilTaxista()
                                        : api.cambiarDisponibilidad(value);
                        MAIN.post(
                                () -> {
                                    busy = false;
                                    if (cerrado) return;
                                    error.setValue(null);
                                    publicar(
                                            DisponibilidadEstado.inicial()
                                                    .servidor(
                                                            p.optBoolean("activo")
                                                                    && "APROBADO"
                                                                            .equals(
                                                                                    p.optString(
                                                                                            "estadoSolicitud")),
                                                            p.opt("disponible")));
                                });
                    } catch (Exception e) {
                        MAIN.post(
                                () -> {
                                    busy = false;
                                    if (cerrado) return;
                                    publicar(
                                            estado.getValue()
                                                    .fallar(
                                                            DisponibilidadEstado.Problema
                                                                    .CONEXION));
                                    error.setValue(TaxiTasks.message(e));
                                });
                    }
                });
    }

    private void publicar(DisponibilidadEstado s) {
        ConductorPreferencias.confirmar(cuenta, s.editable());
        ConductorPreferencias.get(context, cuenta)
                .edit()
                .putBoolean("disponible", s.disponible)
                .putBoolean("disponible_confirmada", s.editable())
                .apply();
        estado.setValue(s);
    }

    public void close() {
        synchronized (DisponibilidadRepository.class) {
            if (cerrado || --usuarios > 0) return;
            INSTANCIAS.remove(cuenta);
            cerrado = true;
            ConductorPreferencias.confirmar(cuenta, false);
        }
    }
}
