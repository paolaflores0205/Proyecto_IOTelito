package com.example.proyecto_iotelito.data.hoteladmin;

import android.content.Context;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;
import com.example.proyecto_iotelito.R;

@Entity(tableName = "habitaciones_admin")
public class HabitacionEntity {
    @PrimaryKey public int id;
    public int hotelId;
    public String tipo;
    public String nombreCodigo;
    public int areaM2;
    public int adultos;
    public int ninos;
    public double precioNoche;
    public String descripcion;
    public String fotoNombre;
    public boolean disponible;

    public static HabitacionEntity from(Context context, Habitacion habitacion) {
        HabitacionEntity entity = new HabitacionEntity();
        entity.id = habitacion.id;
        entity.hotelId = habitacion.hotelId;
        entity.tipo = habitacion.tipo;
        entity.nombreCodigo = habitacion.nombreCodigo;
        entity.areaM2 = habitacion.areaM2;
        entity.adultos = habitacion.adultos;
        entity.ninos = habitacion.ninos;
        entity.precioNoche = habitacion.precioNoche;
        entity.descripcion = habitacion.descripcion;
        entity.fotoNombre = context.getResources().getResourceEntryName(habitacion.fotoRes);
        entity.disponible = habitacion.disponible;
        return entity;
    }

    public Habitacion toModel(Context context) {
        int fotoRes = context.getResources().getIdentifier(
                fotoNombre, "drawable", context.getPackageName());
        if (fotoRes == 0) fotoRes = R.drawable.superadmin_hotel_sol;
        return new Habitacion(id, hotelId, tipo, nombreCodigo, areaM2, adultos, ninos,
                precioNoche, descripcion, fotoRes, disponible);
    }
}
