package com.example.proyecto_iotelito.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface NotificacionDao {

    @Query("SELECT * FROM notificaciones_cliente ORDER BY creadaEn DESC")
    List<NotificacionEntity> obtenerTodas();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void guardarSiNoExiste(NotificacionEntity notificacion);

    @Query("UPDATE notificaciones_cliente SET leida = 1 WHERE id = :id")
    void marcarLeida(int id);

    @Query("UPDATE notificaciones_cliente SET leida = 1")
    void marcarTodasLeidas();
}
