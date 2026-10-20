package com.example.proyecto_iotelito.data.hoteladmin;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "mensajes_admin")
public class MensajeAdminEntity {
    @PrimaryKey(autoGenerate = true)
    public long id;
    public int conversacionId;
    public String texto;
    public long enviadoEn;
}
