package com.example.proyecto_iotelito.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notificaciones_cliente")
public class NotificacionEntity {
    @PrimaryKey
    public int id;
    public String tipo;
    public String titulo;
    public long creadaEn;
    public boolean leida;
    public int reservaId;
}
