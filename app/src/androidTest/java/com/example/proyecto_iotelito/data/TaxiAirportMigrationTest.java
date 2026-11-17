package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.data.local.cliente.TaxiDatabase;
import com.example.proyecto_iotelito.data.local.cliente.TaxiEntity;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import static org.junit.Assert.*;

/** Base aislada de prueba: no abre la base del usuario ni consulta Firebase. */
@RunWith(AndroidJUnit4.class)
public class TaxiAirportMigrationTest {
    @Test public void conservaSolicitudAnteriorYGuardaDestinoNuevo() throws Exception {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="taxi-migration-"+UUID.randomUUID()+".db";
        TaxiDatabase migrated=null;
        try {
            try(SQLiteDatabase old=context.openOrCreateDatabase(name,Context.MODE_PRIVATE,null)){
                old.execSQL("CREATE TABLE servicios_taxi (id INTEGER NOT NULL, reservaId INTEGER NOT NULL, clienteId TEXT, nombreCliente TEXT, hotel TEXT, origen TEXT, destino TEXT, hora TEXT, pasajeros INTEGER NOT NULL, maletas INTEGER NOT NULL, estado TEXT, creado INTEGER NOT NULL, taxistaId TEXT, conductor TEXT, PRIMARY KEY(id))");
                old.execSQL("CREATE UNIQUE INDEX index_servicios_taxi_clienteId_reservaId ON servicios_taxi(clienteId,reservaId)");
                old.execSQL("INSERT INTO servicios_taxi(id,reservaId,clienteId,destino,pasajeros,maletas,creado) VALUES(1,1,'old','Destino anterior',2,1,123)");
                old.setVersion(1);
            }
            migrated=Room.databaseBuilder(context,TaxiDatabase.class,name).addMigrations(TaxiDatabase.MIGRATION_1_2).build();
            TaxiEntity old=migrated.taxiDao().buscar(1);
            assertEquals("Destino anterior",old.destino);assertNull(old.airportCode);assertNull(old.hotelId);
            TaxiEntity fresh=new TaxiEntity();fresh.id=2;fresh.reservaId=2;fresh.clienteId="new";
            fresh.hotelId=20;fresh.hotelCity="Cusco";fresh.airportCode="CUZ";
            fresh.airportName="Aeropuerto Internacional Alejandro Velasco Astete";fresh.airportCity="Cusco";
            fresh.destino=fresh.airportName+" (CUZ)";
            assertTrue(migrated.taxiDao().insertar(fresh)>0);
            TaxiEntity decoded=TaxiEntity.desdeJson(migrated.taxiDao().buscar(2).comoJson()
                    .put("nombre","Cliente").put("hotel","Hotel").put("hora","15:30").put("estado","SOLICITADO"));
            assertEquals("CUZ",decoded.airportCode);assertEquals(Integer.valueOf(20),decoded.hotelId);
            assertEquals("Cusco",decoded.hotelCity);assertEquals(fresh.destino,decoded.destino);
        } finally {
            if(migrated!=null)migrated.close();
            context.deleteDatabase(name);
        }
    }
}
