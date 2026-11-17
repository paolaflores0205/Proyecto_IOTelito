package com.example.proyecto_iotelito.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotificacionDao {
    @Query("SELECT * FROM notificaciones_cliente WHERE ownerUid = :uid ORDER BY creadaEn DESC")
    androidx.lifecycle.LiveData<List<NotificacionEntity>> observarTodas(String uid);

    @Query("SELECT * FROM notificaciones_cliente WHERE ownerUid = :uid ORDER BY creadaEn DESC")
    List<NotificacionEntity> obtenerTodas(String uid);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void guardarSiNoExiste(NotificacionEntity notificacion);

    @Query("UPDATE notificaciones_cliente SET leida = 1 WHERE ownerUid = :uid AND id = :id")
    void marcarLeida(String uid, int id);

    @Query("UPDATE notificaciones_cliente SET leida = 1 WHERE ownerUid = :uid")
    void marcarTodasLeidas(String uid);
}
