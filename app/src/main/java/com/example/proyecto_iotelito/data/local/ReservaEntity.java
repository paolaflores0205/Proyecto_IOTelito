package com.example.proyecto_iotelito.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Fila persistida de una reserva del cliente. Las fechas se guardan en ISO-8601. */
@Entity(tableName = "reservas_cliente")
public class ReservaEntity {

    @PrimaryKey
    public int id;
    public int hotelId;
    public String roomName;
    public String roomNumber;
    public String fechaEntrada;
    public String fechaSalida;
    public String huespedes;
    public double precioTotal;
    public String estado;
    public String codigo;
}
