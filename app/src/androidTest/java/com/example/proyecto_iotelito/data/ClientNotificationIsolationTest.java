package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.example.proyecto_iotelito.data.local.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import static org.junit.Assert.*;

/** Base temporal propia; nunca abre la del usuario ni consulta Firebase. */
@RunWith(AndroidJUnit4.class)
public class ClientNotificationIsolationTest {
    @Test public void migraSinAdjudicarAvisosAntiguosYSeparaLecturaPorCuenta() {
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        String name="notification-isolation-"+UUID.randomUUID()+".db";
        IoTelitoDatabase db=null;
        try {
            try(SQLiteDatabase old=context.openOrCreateDatabase(name,Context.MODE_PRIVATE,null)) {
                old.execSQL("CREATE TABLE reservas_cliente (id INTEGER NOT NULL, hotelId INTEGER NOT NULL, roomName TEXT, roomNumber TEXT, fechaEntrada TEXT, fechaSalida TEXT, huespedes TEXT, precioTotal REAL NOT NULL, estado TEXT, codigo TEXT, PRIMARY KEY(id))");
                old.execSQL("INSERT INTO reservas_cliente(id,hotelId,precioTotal,codigo) VALUES(42,7,1000,'CONSERVAR')");
                old.execSQL("CREATE TABLE notificaciones_cliente (id INTEGER NOT NULL, tipo TEXT, titulo TEXT, creadaEn INTEGER NOT NULL, leida INTEGER NOT NULL, reservaId INTEGER NOT NULL, PRIMARY KEY(id))");
                old.execSQL("INSERT INTO notificaciones_cliente VALUES(1,'RESERVA','Aviso sin dueÃ±o',123,0,42)");
                old.setVersion(2);
            }
            db=Room.databaseBuilder(context,IoTelitoDatabase.class,name).addMigrations(IoTelitoDatabase.MIGRATION_2_3).build();
            NotificacionDao dao=db.notificacionDao();
            assertTrue(dao.obtenerTodas("alice").isEmpty());assertTrue(dao.obtenerTodas("bob").isEmpty());
            assertEquals(1,dao.obtenerTodas("").size());
            try(android.database.Cursor cursor=db.getOpenHelper().getReadableDatabase().query("SELECT codigo FROM reservas_cliente WHERE id=42")) {
                assertTrue(cursor.moveToFirst());assertEquals("CONSERVAR",cursor.getString(0));
            }
            NotificacionEntity a=new NotificacionEntity();a.ownerUid="alice";a.id=1;a.tipo="TAXI";a.titulo="Solo Alice";a.reservaId=42;
            NotificacionEntity b=new NotificacionEntity();b.ownerUid="bob";b.id=1;b.tipo="TAXI";b.titulo="Solo Bob";b.reservaId=99;
            dao.guardarSiNoExiste(a);dao.guardarSiNoExiste(b);
            assertEquals("Solo Alice",dao.obtenerTodas("alice").get(0).titulo);
            assertEquals("Solo Bob",dao.obtenerTodas("bob").get(0).titulo);
            dao.marcarLeida("alice",1);
            assertTrue(dao.obtenerTodas("alice").get(0).leida);assertFalse(dao.obtenerTodas("bob").get(0).leida);
            dao.marcarTodasLeidas("bob");assertTrue(dao.obtenerTodas("bob").get(0).leida);
            assertFalse(dao.obtenerTodas("").get(0).leida);
        } finally { if(db!=null)db.close();context.deleteDatabase(name); }
    }
}
