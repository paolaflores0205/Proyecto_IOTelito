package com.example.proyecto_iotelito.data.local;

import androidx.room.Entity;
import androidx.annotation.NonNull;

@Entity(tableName = "notificaciones_cliente", primaryKeys = {"ownerUid", "id"})
public class NotificacionEntity {
    @NonNull public String ownerUid = "";
    public int id;
    public String tipo;
    public String titulo;
    public long creadaEn;
    public boolean leida;
    public int reservaId;
}
