package com.example.proyecto_iotelito.data.taxista;

import android.content.Context;
import android.util.Log;
import androidx.lifecycle.*;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.data.taxista.local.*;
import com.example.proyecto_iotelito.model.auth.*;
import com.example.proyecto_iotelito.model.taxista.*;
import com.example.proyecto_iotelito.ui.taxista.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

/** Clase 7 extra: Repository + Room; I/O serial fuera del hilo UI. */
public final class ConductorRepository {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private final Context context;
    private final ConductorDatabase db;
    private final ConductorDao dao;
    public final String cuenta;
    public final LiveData<List<ServicioConductorEntity>> servicios;
    public final LiveData<List<AvisoConductorEntity>> avisos;
    public final MutableLiveData<String> error=new MutableLiveData<>();
    public ConductorRepository(Context context) {this(context,ConductorDatabase.get(context),true);}
    // Base inyectable para comprobar almacenamiento sin alterar los datos del usuario.
    ConductorRepository(Context context,ConductorDatabase database,boolean compartir) {
        this.context=context.getApplicationContext();
        AuthenticatedUser u=SessionRepository.obtener(context);
        cuenta=u!=null&&u.getRole()==UserRole.TAXISTA ? u.getEmail() : "";
        db=database; dao=db.dao();
        servicios=dao.observar(cuenta); avisos=dao.avisos(cuenta);
        if(!cuenta.isEmpty()) IO.execute(()->{try { seed(); if(compartir)sincronizar(); } catch(Exception e) {fallo(e);} });
    }
    private void autorizar() {
        AuthenticatedUser u=SessionRepository.obtener(context);
        if(u==null||u.getRole()!=UserRole.TAXISTA||!cuenta.equals(u.getEmail())) throw new IllegalStateException("Sesión de taxista requerida");
    }
    private void fallo(Exception e) {
        Log.e("TaxistaLocal","No se pudo completar la operación",e);
        error.postValue(context.getString(R.string.taxista_local_error));
    }
    public <T> void ejecutar(LifecycleOwner owner,Callable<T> tarea,Consumer<T> resultado) {
        MutableLiveData<Resultado<T>> live=new MutableLiveData<>();
        live.observe(owner,r->{live.removeObservers(owner); if(r.error!=null) fallo(r.error); else resultado.accept(r.valor);});
        IO.execute(()->{try {autorizar(); live.postValue(new Resultado<>(tarea.call(),null));}
            catch(Exception e) {live.postValue(new Resultado<>(null,e));} });
    }
    private static class Resultado<T> { T valor; Exception error; Resultado(T v,Exception e){valor=v;error=e;} }
    private void seed() {
        autorizar();
        db.runInTransaction(()->{
            if(dao.inicio(cuenta)!=null) return;
            for(SolicitudServicio s:TaxistaSampleData.solicitudes()) dao.guardar(ServicioConductorEntity.from(cuenta,s,false));
            for(ServicioHistorial h:TaxistaSampleData.historial()) {
                ServicioConductorEntity e=new ServicioConductorEntity(); e.cuenta=cuenta; e.id=-h.id;
                e.estado=h.estado.name(); e.hotel=h.origen; e.origen=h.origen; e.destino=h.destino;
                e.nombre=h.pasajero; e.iniciales=h.pasajero.substring(0,1); e.fecha=h.hora;
                e.distancia=h.distancia; e.duracion=h.duracion; e.motivo=h.motivoCancelacion; e.tarifa=h.monto;
                dao.guardar(e);
            }
            InicioConductorEntity inicio=new InicioConductorEntity(); inicio.cuenta=cuenta; dao.iniciar(inicio);
        });
    }
    public void refrescar() { IO.execute(()->{try {autorizar(); sincronizar();} catch(Exception e){fallo(e);} }); }
    private void sincronizar() {
        // Los ejemplos se cargan primero: una falla del repositorio compartido no los oculta.
        for(ServicioConductorEntity e:dao.todos(cuenta)) {
            if(!e.cliente) continue;
            org.json.JSONObject p=TaxiRepository.buscar(context,e.id);
            if(p==null||(!"SOLICITADO".equals(p.optString("estado"))&&!cuenta.equals(p.optString("taxista")))){
                dao.retirarCompartido(cuenta,e.id);dao.quitarSolicitud(cuenta,e.id);
                TaxistaNotificaciones.cancelar(context,cuenta,e.id);continue;
            }
            if("FINALIZADO".equals(p.optString("estado"))&&!"FINALIZADO".equals(e.estado)) {
                e.estado="FINALIZADO"; e.finalizado=System.currentTimeMillis(); dao.guardar(e);
            }
        }
        for(SolicitudServicio s:TaxiRepository.disponibles(context)) {
            ServicioConductorEntity e=dao.buscar(cuenta,s.id);
            org.json.JSONObject p=TaxiRepository.buscar(context,s.id);
            if(p==null) continue;
            if(e==null) e=ServicioConductorEntity.from(cuenta,s,true);
            e.minutos=s.minutosEspera; e.origen=p.optString("origen",s.hotel);
            String compartido=p.optString("estado","SOLICITADO");
            if(!("EN_TRASLADO".equals(e.estado)&&"EN_CAMINO".equals(compartido))) e.estado=compartido;
            dao.guardar(e);
        }
        for(ServicioConductorEntity e:dao.todos(cuenta)) if("SOLICITADO".equals(e.estado))
            aviso(e,"solicitud",context.getString(R.string.taxista_local_nueva),e.nombre+" · "+e.hotel);
        publicar();
    }
    public ServicioConductorEntity buscar(int id) {return dao.buscar(cuenta,id);}
    public ServicioConductorEntity activo() {return dao.activo(cuenta);}
    public boolean aceptar(int id) {
        autorizar();
        if(!ConductorPreferencias.disponible(context,cuenta)) return false;
        ServicioConductorEntity e=buscar(id);
        if(e==null||!"SOLICITADO".equals(e.estado)||activo()!=null) return false;
        if(e.cliente&&!TaxiRepository.avanzar(context,id,TaxiRepository.Estado.ASIGNADO)) return false;
        db.runInTransaction(()->{e.estado="ASIGNADO"; dao.guardar(e); dao.quitarSolicitud(cuenta,id);});
        TaxistaNotificaciones.cancelar(context,cuenta,id);
        return true;
    }
    public boolean avanzar(int id,EstadoServicio siguiente) {
        autorizar(); ServicioConductorEntity e=buscar(id);
        if(e==null) return false;
        boolean valido=("ASIGNADO".equals(e.estado)&&siguiente==EstadoServicio.EN_CAMINO)
                ||("EN_CAMINO".equals(e.estado)&&siguiente==EstadoServicio.EN_TRASLADO)
                ||("EN_TRASLADO".equals(e.estado)&&siguiente==EstadoServicio.FINALIZADO);
        if(!valido) return false;
        if(e.cliente&&siguiente!=EstadoServicio.EN_TRASLADO) {
            TaxiRepository.Estado compartido=siguiente==EstadoServicio.EN_CAMINO ? TaxiRepository.Estado.EN_CAMINO : TaxiRepository.Estado.FINALIZADO;
            org.json.JSONObject p=TaxiRepository.buscar(context,id);
            // Permite reintentar una escritura local si el cambio compartido ya se confirmó.
            if(p==null||(!compartido.name().equals(p.optString("estado"))&&!TaxiRepository.avanzar(context,id,compartido))) return false;
        }
        db.runInTransaction(()->{
            e.estado=siguiente.name(); if(siguiente==EstadoServicio.FINALIZADO) e.finalizado=System.currentTimeMillis();
            dao.guardar(e);
            if(siguiente==EstadoServicio.FINALIZADO) aviso(e,"finalizado",context.getString(R.string.taxista_local_fin),e.nombre+" · "+e.destino);
        });
        publicar(); return true;
    }
    private void aviso(ServicioConductorEntity e,String tipo,String titulo,String texto) {
        AvisoConductorEntity a=new AvisoConductorEntity(); a.cuenta=cuenta; a.clave=tipo+":"+e.id;
        a.servicio=e.id; a.tipo=tipo; a.titulo=titulo; a.texto=texto; a.fecha=System.currentTimeMillis(); dao.aviso(a);
    }
    public void publicar() {
        autorizar();
        for(AvisoConductorEntity a:dao.pendientes(cuenta)) {
            if("solicitud".equals(a.tipo)&&!ConductorPreferencias.disponible(context,cuenta))continue;
            // Un fallo de notificación no revierte el servicio guardado.
            if(TaxistaNotificaciones.publicar(context,a)) dao.publicado(cuenta,a.clave);
        }
    }
    public void leer(String clave) {IO.execute(()->{try {autorizar();dao.leer(cuenta,clave);} catch(Exception e){fallo(e);} });}
    public LiveData<List<MensajeConductorEntity>> mensajes(int id) {return dao.mensajes(cuenta,id);}
    public void mensaje(int id,String texto,boolean mio) {
        autorizar(); if(buscar(id)==null||texto.trim().isEmpty()) return;
        MensajeConductorEntity m=new MensajeConductorEntity(); m.cuenta=cuenta; m.servicio=id;
        m.texto=texto.trim(); m.mio=mio; m.fecha=System.currentTimeMillis(); dao.mensaje(m);
    }
}
