package com.example.proyecto_iotelito.data.local.cliente;

import android.content.SharedPreferences;
import androidx.annotation.WorkerThread;
import org.json.JSONException;
import org.json.JSONObject;

public final class TaxiLegacyMigration {
    private TaxiLegacyMigration() { }

    @WorkerThread
    public static void migrar(TaxiDatabase database, SharedPreferences prefs) {
        if (prefs.getBoolean("migrado_room", false)) return;
        TaxiDao dao = database.taxiDao();
        database.runInTransaction(() -> {
            for (String key : prefs.getAll().keySet()) {
                if (!key.startsWith("pedido_")) continue;
                try {
                    TaxiEntity entity = TaxiEntity.desdeJson(new JSONObject(prefs.getString(key, "")));
                    dao.insertar(entity);
                    TaxiEntity guardada = dao.buscar(entity.id);
                    if (guardada == null || !entity.clienteId.equals(guardada.clienteId)
                            || entity.reservaId != guardada.reservaId) {
                        throw new IllegalStateException("Conflicto al migrar la solicitud " + entity.id);
                    }
                } catch (JSONException exception) {
                    throw new IllegalStateException("No se pudo migrar " + key, exception);
                }
            }
        });
        prefs.edit().putBoolean("migrado_room", true).apply();
    }
}
