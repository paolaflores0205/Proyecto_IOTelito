package com.example.proyecto_iotelito.model.hoteladmin;

import java.io.Serializable;

/**
 * Servicio adicional que ofrece un hotel (desayuno, lavandería, traslado, spa…).
 * {@link #activo} no es final: el admin puede activar o desactivar el servicio.
 */
public class Servicio implements Serializable {

    public final int id;
    public final int hotelId;
    public final String nombre;
    public final String descripcion;
    public final double precio;
    public final int fotoRes;
    public boolean activo;

    public Servicio(int id, int hotelId, String nombre, String descripcion, double precio,
                    int fotoRes, boolean activo) {
        this.id = id;
        this.hotelId = hotelId;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precio = precio;
        this.fotoRes = fotoRes;
        this.activo = activo;
    }
}
