package com.example.proyecto_iotelito.data.hoteladmin;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HotelAdminDao {
    @Query("SELECT * FROM habitaciones_admin ORDER BY id")
    List<HabitacionEntity> habitaciones();

    @Query("SELECT * FROM servicios_admin ORDER BY id")
    List<ServicioEntity> servicios();

    @Query("SELECT * FROM checkouts_admin")
    List<CheckoutEntity> checkouts();

    @Query("SELECT * FROM mensajes_admin WHERE conversacionId = :conversacionId ORDER BY enviadoEn, id")
    List<MensajeAdminEntity> mensajes(int conversacionId);

    @Insert
    void guardarMensaje(MensajeAdminEntity mensaje);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarHabitaciones(List<HabitacionEntity> habitaciones);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarServicios(List<ServicioEntity> servicios);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarHabitacion(HabitacionEntity habitacion);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarServicio(ServicioEntity servicio);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void guardarCheckout(CheckoutEntity checkout);

    @Query("DELETE FROM habitaciones_admin WHERE id = :id")
    void eliminarHabitacion(int id);

    @Query("DELETE FROM servicios_admin WHERE id = :id")
    void eliminarServicio(int id);
}
