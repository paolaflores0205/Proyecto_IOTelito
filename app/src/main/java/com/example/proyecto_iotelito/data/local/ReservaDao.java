package com.example.proyecto_iotelito.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ReservaDao {
    @Query("SELECT * FROM reservas_cliente ORDER BY fechaEntrada ASC")
    androidx.lifecycle.LiveData<List<ReservaEntity>> observarTodas();

    @Query("SELECT * FROM reservas_cliente ORDER BY fechaEntrada ASC")
    List<ReservaEntity> obtenerTodas();

    @Query("SELECT * FROM reservas_cliente WHERE id = :id LIMIT 1")
    ReservaEntity buscarPorId(int id);

    @Query("SELECT COALESCE(MAX(id), 0) FROM reservas_cliente")
    int obtenerIdMaximo();

    @Query("SELECT COUNT(*) FROM reservas_cliente")
    int contar();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardar(ReservaEntity reserva);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarTodas(List<ReservaEntity> reservas);

    @Query("UPDATE reservas_cliente SET estado = :estado WHERE id = :reservaId")
    void actualizarEstado(int reservaId, String estado);
}
