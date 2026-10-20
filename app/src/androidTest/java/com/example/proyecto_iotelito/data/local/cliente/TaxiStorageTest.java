package com.example.proyecto_iotelito.data.local.cliente;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class TaxiStorageTest {
    private Context context;
    private TaxiDatabase database;
    private SharedPreferences prefs;
    private String nombre;

    @Before
    public void preparar() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        nombre = "test_taxi_" + UUID.randomUUID() + ".db";
        prefs = context.getSharedPreferences(nombre, Context.MODE_PRIVATE);
        database = Room.databaseBuilder(context, TaxiDatabase.class, nombre).build();
    }

    @After
    public void limpiar() {
        if (database != null) database.close();
        context.deleteDatabase(nombre);
        context.deleteSharedPreferences(nombre);
    }

    private TaxiEntity solicitud(int id, int reservaId) throws Exception {
        return TaxiEntity.desdeJson(new JSONObject()
                .put("id", id).put("reserva", reservaId).put("cliente", "cliente@iotelito.pe")
                .put("nombre", "Carlos").put("hotel", "Hotel de prueba").put("hora", "15:30")
                .put("pasajeros", 2).put("maletas", 3).put("estado", "SOLICITADO").put("creado", 123456L));
    }

    @Test
    public void persisteAlCerrarYReabrirLaBase() throws Exception {
        database.taxiDao().insertar(solicitud(100000, 1));
        database.close();
        database = Room.databaseBuilder(context, TaxiDatabase.class, nombre).build();
        TaxiEntity guardada = database.taxiDao().buscar(100000);
        assertEquals("cliente@iotelito.pe", guardada.clienteId);
        assertEquals(3, guardada.maletas);
        assertEquals("Aeropuerto Jorge Chávez", guardada.destino);
        assertEquals(100001, database.taxiDao().siguienteId());
    }

    @Test
    public void evitaDuplicarReservaYProtegeLaAsignacion() throws Exception {
        TaxiDao dao = database.taxiDao();
        dao.insertar(solicitud(100000, 1));
        assertEquals(-1L, dao.insertar(solicitud(100001, 1)));
        assertEquals(1, dao.avanzar(100000, "SOLICITADO", "ASIGNADO", "taxista1", "Conductor 1"));
        assertEquals(0, dao.avanzar(100000, "SOLICITADO", "ASIGNADO", "taxista2", "Conductor 2"));
        assertEquals(0, dao.avanzar(100000, "ASIGNADO", "EN_CAMINO", "taxista2", "Conductor 2"));
        assertEquals(1, dao.avanzar(100000, "ASIGNADO", "EN_CAMINO", "taxista1", "Conductor 1"));
        assertEquals(0, dao.disponibles("taxista2").size());
        assertEquals(1, dao.disponibles("taxista1").size());
        assertEquals(1, dao.avanzar(100000, "EN_CAMINO", "FINALIZADO", "taxista1", "Conductor 1"));
        assertEquals(0, dao.disponibles("taxista1").size());
    }

    @Test
    public void importaPreferenciasSinCambiarIdsNiSobrescribirEstados() throws Exception {
        prefs.edit().putString("pedido_100005", solicitud(100005, 1).comoJson().toString()).commit();
        TaxiLegacyMigration.migrar(database, prefs);
        assertTrue(prefs.getBoolean("migrado_room", false));
        assertTrue(prefs.contains("pedido_100005"));
        assertEquals(100005, database.taxiDao().porReserva("cliente@iotelito.pe", 1).id);
        database.taxiDao().avanzar(100005, "SOLICITADO", "ASIGNADO", "taxista1", "Conductor 1");
        prefs.edit().remove("migrado_room").commit();
        TaxiLegacyMigration.migrar(database, prefs);
        assertEquals("ASIGNADO", database.taxiDao().buscar(100005).estado);
    }

    @Test
    public void datosInvalidosNoDejanUnaMigracionParcial() throws Exception {
        prefs.edit().putString("pedido_100000", solicitud(100000, 1).comoJson().toString())
                .putString("pedido_100001", "dato inválido").commit();
        assertThrows(IllegalStateException.class, () -> TaxiLegacyMigration.migrar(database, prefs));
        assertNull(database.taxiDao().buscar(100000));
        assertFalse(prefs.getBoolean("migrado_room", false));
        assertTrue(prefs.contains("pedido_100000"));
    }
}
