package com.example.proyecto_iotelito.data.hoteladmin;

import android.content.Context;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.example.proyecto_iotelito.model.hoteladmin.Servicio;
import com.example.proyecto_iotelito.R;

@Entity(tableName = "servicios_admin")
public class ServicioEntity {
    @PrimaryKey public int id;
    public int hotelId;
    public String nombre;
    public String descripcion;
    public double precio;
    public String unidad;
    public String fotoNombre;
    public boolean activo;

    public static ServicioEntity from(Context context, Servicio servicio) {
        ServicioEntity entity = new ServicioEntity();
        entity.id = servicio.id;
        entity.hotelId = servicio.hotelId;
        entity.nombre = servicio.nombre;
        entity.descripcion = servicio.descripcion;
        entity.precio = servicio.precio;
        entity.unidad = servicio.unidad;
        entity.fotoNombre = context.getResources().getResourceEntryName(servicio.fotoRes);
        entity.activo = servicio.activo;
        return entity;
    }

    public Servicio toModel(Context context) {
        int fotoRes = context.getResources().getIdentifier(
                fotoNombre, "drawable", context.getPackageName());
        if (fotoRes == 0) fotoRes = R.drawable.superadmin_hotel_sol;
        return new Servicio(id, hotelId, nombre, descripcion, precio, unidad, fotoRes, activo);
    }
}
