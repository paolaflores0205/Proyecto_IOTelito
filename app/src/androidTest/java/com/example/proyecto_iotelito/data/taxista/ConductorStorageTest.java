package com.example.proyecto_iotelito.data.taxista;

import android.content.Context;
import android.app.NotificationManager;
import androidx.lifecycle.*;
import androidx.room.Room;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.data.taxista.local.*;
import com.example.proyecto_iotelito.model.auth.*;
import com.example.proyecto_iotelito.ui.taxista.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ConductorStorageTest {
    private Context context;
    private ConductorDatabase db;
    private ConductorRepository repo;
    private String name,cuenta;
    private final LifecycleOwner owner=new LifecycleOwner(){
        final LifecycleRegistry lifecycle=new LifecycleRegistry(this);
        @Override public Lifecycle getLifecycle(){return lifecycle;}
    };
    @Before public void setup() throws Exception {
        context=InstrumentationRegistry.getInstrumentation().getTargetContext();
        name="test_conductor_"+UUID.randomUUID()+".db";cuenta=UUID.randomUUID()+"@taxista.test";
        SessionRepository.guardar(context,new AuthenticatedUser(cuenta,"Conductor de prueba",UserRole.TAXISTA));
        db=Room.databaseBuilder(context,ConductorDatabase.class,name).build();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{
            ((LifecycleRegistry)owner.getLifecycle()).setCurrentState(Lifecycle.State.RESUMED);
            repo=new ConductorRepository(context,db,false);
        });
        call(()->repo.buscar(1));
    }
    private <T> T call(Callable<T> action) throws Exception {
        CountDownLatch latch=new CountDownLatch(1);AtomicReference<T> result=new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->repo.ejecutar(owner,action,r->{result.set(r);latch.countDown();}));
        assertTrue("La operación debe completar",latch.await(15,TimeUnit.SECONDS));return result.get();
    }
    @After public void cleanup() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->((LifecycleRegistry)owner.getLifecycle()).setCurrentState(Lifecycle.State.DESTROYED));
        NotificationManager nm=context.getSystemService(NotificationManager.class);
        for(int id=1;id<=6;id++){nm.cancel("taxista:"+cuenta+":solicitud:"+id,0);nm.cancel("taxista:"+cuenta+":finalizado:"+id,0);}
        db.close();context.deleteDatabase(name);context.deleteSharedPreferences("taxista_"+cuenta);SessionRepository.cerrar(context);
    }
    @Test public void conservaEjemplosYNoLosReinsertaAlReabrir() throws Exception {
        assertEquals(13,db.dao().todos(cuenta).size());
        assertTrue(call(()->repo.aceptar(2)));
        db.close();db=Room.databaseBuilder(context,ConductorDatabase.class,name).build();
        assertEquals("ASIGNADO",db.dao().buscar(cuenta,2).estado);
        assertEquals("Carlos Mendoza",db.dao().buscar(cuenta,2).nombre);
        assertEquals(13,db.dao().todos(cuenta).size());
    }
    @Test public void unSoloActivoYTransicionesSinDuplicados() throws Exception {
        assertTrue(call(()->repo.aceptar(3)));
        assertFalse(call(()->repo.aceptar(1)));
        assertFalse(call(()->repo.avanzar(3,EstadoServicio.FINALIZADO)));
        assertTrue(call(()->repo.avanzar(3,EstadoServicio.EN_CAMINO)));
        assertTrue(call(()->repo.avanzar(3,EstadoServicio.EN_TRASLADO)));
        db.close();db=Room.databaseBuilder(context,ConductorDatabase.class,name).build();
        assertEquals("EN_TRASLADO",db.dao().activo(cuenta).estado);
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->repo=new ConductorRepository(context,db,false));
        assertTrue(call(()->repo.avanzar(3,EstadoServicio.FINALIZADO)));
        assertFalse(call(()->repo.avanzar(3,EstadoServicio.FINALIZADO)));
        assertNull(db.dao().activo(cuenta));
        assertEquals(13,db.dao().todos(cuenta).size());
        assertEquals("Rodrigo Salas",db.dao().buscar(cuenta,3).historial().pasajero);
        try(android.database.Cursor c=db.query(new androidx.sqlite.db.SimpleSQLiteQuery(
                "SELECT COUNT(*) FROM conductor_avisos WHERE cuenta=? AND clave='finalizado:3'",new Object[]{cuenta}))){
            assertTrue(c.moveToFirst());assertEquals(1,c.getInt(0));
        }
    }
    @Test public void aislamientoPorCuentaYMensajesConFecha() throws Exception {
        call(()->{repo.mensaje(1,"Estoy en recepción",true);return true;});
        CountDownLatch latch=new CountDownLatch(1);
        LiveData<List<MensajeConductorEntity>> source=db.dao().mensajes(cuenta,1);
        androidx.lifecycle.Observer<List<MensajeConductorEntity>> observer=rows->{if(!rows.isEmpty()){assertEquals("Estoy en recepción",rows.get(0).texto);assertTrue(rows.get(0).fecha>0);latch.countDown();}};
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->source.observeForever(observer));
        assertTrue(latch.await(10,TimeUnit.SECONDS));
        InstrumentationRegistry.getInstrumentation().runOnMainSync(()->source.removeObserver(observer));
        assertNull(db.dao().buscar("otra@cuenta",1));
        assertTrue(db.dao().todos("otra@cuenta").isEmpty());
    }
    @Test public void pedidoCompartidoConservaLaConexionDelCliente() throws Exception {
        com.example.proyecto_iotelito.data.local.cliente.TaxiDatabase compartida=
                com.example.proyecto_iotelito.data.local.cliente.TaxiDatabase.getInstance(context);
        com.example.proyecto_iotelito.data.local.cliente.TaxiEntity e=new com.example.proyecto_iotelito.data.local.cliente.TaxiEntity();
        e.id=compartida.taxiDao().siguienteId();e.reservaId=2345678;
        e.clienteId=cuenta+".cliente";e.nombreCliente="Pasajero de integración";
        e.hotel="Hotel de integración";e.origen="Dirección de integración";e.destino="Aeropuerto";
        e.hora="23:59";e.estado="SOLICITADO";e.creado=System.currentTimeMillis();e.taxistaId="";e.conductor="";
        compartida.taxiDao().insertar(e);
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(()->repo=new ConductorRepository(context,db,true));
            assertNotNull(call(()->repo.buscar(e.id)));
            assertTrue(call(()->repo.aceptar(e.id)));
            assertEquals("ASIGNADO",compartida.taxiDao().buscar(e.id).estado);
            assertTrue(call(()->repo.avanzar(e.id,EstadoServicio.EN_CAMINO)));
            assertTrue(call(()->repo.avanzar(e.id,EstadoServicio.EN_TRASLADO)));
            assertEquals("EN_TRASLADO",db.dao().buscar(cuenta,e.id).estado);
            assertEquals("EN_CAMINO",compartida.taxiDao().buscar(e.id).estado);
            assertTrue(call(()->repo.avanzar(e.id,EstadoServicio.FINALIZADO)));
            assertEquals("FINALIZADO",compartida.taxiDao().buscar(e.id).estado);
            assertEquals("Pasajero de integración",db.dao().buscar(cuenta,e.id).historial().pasajero);
        } finally {
            java.util.concurrent.CountDownLatch barrier=new java.util.concurrent.CountDownLatch(1);
            ClientLocalTasks.execute(barrier::countDown);assertTrue(barrier.await(10,java.util.concurrent.TimeUnit.SECONDS));
            compartida.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM servicios_taxi WHERE id=? AND clienteId=?",new Object[]{e.id,e.clienteId});
            for(int estado=0;estado<4;estado++){
                int id=1000000+e.id*4+estado;
                context.getSystemService(NotificationManager.class).cancel(id);
                com.example.proyecto_iotelito.data.local.IoTelitoDatabase.getInstance(context).getOpenHelper()
                        .getWritableDatabase().execSQL("DELETE FROM notificaciones_cliente WHERE id=?",new Object[]{id});
            }
            context.getSystemService(NotificationManager.class).cancel("taxista:"+cuenta+":solicitud:"+e.id,0);
            context.getSystemService(NotificationManager.class).cancel("taxista:"+cuenta+":finalizado:"+e.id,0);
        }
    }
    @Test public void preferenciasYCanalPropios() throws Exception {
        ConductorPreferencias.get(context,cuenta).edit().putBoolean("disponible",false).commit();
        assertFalse(ConductorPreferencias.disponible(context,cuenta));
        assertFalse(call(()->repo.aceptar(1)));
        assertEquals("SOLICITADO",db.dao().buscar(cuenta,1).estado);
        assertNull(db.dao().activo(cuenta));
        ConductorPreferencias.get(context,cuenta).edit().putBoolean("disponible",true).commit();
        assertTrue(call(()->repo.aceptar(1)));
        assertTrue(ConductorPreferencias.disponible(context,"otra@cuenta"));
        TaxistaNotificaciones.canal(context);
        assertNotNull(context.getSystemService(NotificationManager.class).getNotificationChannel(TaxistaNotificaciones.CHANNEL));
    }
}
