package com.example.proyecto_iotelito.data.local.cliente;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TaxiDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertar(TaxiEntity solicitud);

    @Query("SELECT * FROM servicios_taxi WHERE id = :id")
    TaxiEntity buscar(int id);

    @Query("SELECT * FROM servicios_taxi WHERE id = :id")
    LiveData<TaxiEntity> observar(int id);

    @Query("SELECT * FROM servicios_taxi WHERE clienteId = :clienteId AND reservaId = :reservaId LIMIT 1")
    TaxiEntity porReserva(String clienteId, int reservaId);

    @Query("SELECT MAX(100000, COALESCE(MAX(id) + 1, 100000)) FROM servicios_taxi")
    int siguienteId();

    @Query("SELECT * FROM servicios_taxi WHERE estado = 'SOLICITADO' "
            + "OR (taxistaId = :taxistaId AND estado != 'FINALIZADO') ORDER BY creado, id")
    List<TaxiEntity> disponibles(String taxistaId);

    @Query("UPDATE servicios_taxi SET estado = :siguiente, taxistaId = :taxistaId, conductor = :conductor "
            + "WHERE id = :id AND estado = :actual AND (estado = 'SOLICITADO' OR taxistaId = :taxistaId)")
    int avanzar(int id, String actual, String siguiente, String taxistaId, String conductor);
}
