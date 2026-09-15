package com.example.proyecto_iotelito.model.hoteladmin;

import java.io.Serializable;

/**
 * Habitación (o tipo de habitación) de un hotel administrado.
 * Datos estáticos en memoria mientras no exista la persistencia NoSQL/Firebase.
 * {@link #disponible} no es final: el admin puede alternar su disponibilidad.
 */
public class Habitacion implements Serializable {

    public final int id;
    public final int hotelId;
    public final String tipo;
    public final String nombreCodigo;
    public final int areaM2;
    public final int adultos;
    public final int ninos;
    public final double precioNoche;
    public final String descripcion;
    public final int fotoRes;
    public boolean disponible;

    public Habitacion(int id, int hotelId, String tipo, String nombreCodigo, int areaM2,
                      int adultos, int ninos, double precioNoche, String descripcion,
                      int fotoRes, boolean disponible) {
        this.id = id;
        this.hotelId = hotelId;
        this.tipo = tipo;
        this.nombreCodigo = nombreCodigo;
        this.areaM2 = areaM2;
        this.adultos = adultos;
        this.ninos = ninos;
        this.precioNoche = precioNoche;
        this.descripcion = descripcion;
        this.fotoRes = fotoRes;
        this.disponible = disponible;
    }

    /** Aforo legible, ej. "2 adultos, 1 niño". */
    public String aforo() {
        String base = adultos + (adultos == 1 ? " adulto" : " adultos");
        if (ninos > 0) {
            base += ", " + ninos + (ninos == 1 ? " niño" : " niños");
        }
        return base;
    }
}
