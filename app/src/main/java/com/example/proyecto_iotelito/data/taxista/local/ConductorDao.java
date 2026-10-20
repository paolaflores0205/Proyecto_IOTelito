package com.example.proyecto_iotelito.data.taxista.local;
import androidx.lifecycle.LiveData;
import androidx.room.*;
import java.util.List;
@Dao
public interface ConductorDao {
    @Query("SELECT * FROM conductor_inicio WHERE cuenta=:cuenta") InicioConductorEntity inicio(String cuenta);
    @Insert void iniciar(InicioConductorEntity inicio);
    @Insert(onConflict=OnConflictStrategy.REPLACE) void guardar(ServicioConductorEntity servicio);
    @Query("DELETE FROM conductor_servicios WHERE cuenta=:cuenta AND id=:id AND cliente=1") void retirarCompartido(String cuenta,int id);
    @Query("SELECT * FROM conductor_servicios WHERE cuenta=:cuenta ORDER BY finalizado DESC,id")
    LiveData<List<ServicioConductorEntity>> observar(String cuenta);
    @Query("SELECT * FROM conductor_servicios WHERE cuenta=:cuenta AND id=:id") ServicioConductorEntity buscar(String cuenta,int id);
    @Query("SELECT * FROM conductor_servicios WHERE cuenta=:cuenta") List<ServicioConductorEntity> todos(String cuenta);
    @Query("SELECT * FROM conductor_servicios WHERE cuenta=:cuenta AND estado IN ('ASIGNADO','EN_CAMINO','EN_TRASLADO') LIMIT 1")
    ServicioConductorEntity activo(String cuenta);
    @Insert(onConflict=OnConflictStrategy.IGNORE) long aviso(AvisoConductorEntity aviso);
    @Query("SELECT * FROM conductor_avisos WHERE cuenta=:cuenta ORDER BY fecha DESC,clave") LiveData<List<AvisoConductorEntity>> avisos(String cuenta);
    @Query("SELECT * FROM conductor_avisos WHERE cuenta=:cuenta AND publicado=0") List<AvisoConductorEntity> pendientes(String cuenta);
    @Query("UPDATE conductor_avisos SET publicado=1 WHERE cuenta=:cuenta AND clave=:clave") void publicado(String cuenta,String clave);
    @Query("UPDATE conductor_avisos SET leido=1 WHERE cuenta=:cuenta AND clave=:clave") void leer(String cuenta,String clave);
    @Query("DELETE FROM conductor_avisos WHERE cuenta=:cuenta AND servicio=:id AND tipo='solicitud'") void quitarSolicitud(String cuenta,int id);
    @Insert void mensaje(MensajeConductorEntity mensaje);
    @Query("SELECT * FROM conductor_mensajes WHERE cuenta=:cuenta AND servicio=:id ORDER BY id")
    LiveData<List<MensajeConductorEntity>> mensajes(String cuenta,int id);
}
