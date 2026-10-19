package com.example.proyecto_iotelito.data.superadmin.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Bandeja local administrativa. Una clave estable evita repetir la misma alerta. */
@Entity(tableName = "admin_alerts")
public class AdminAlertEntity {
    @PrimaryKey @NonNull public String key = "";
    public String title, message;
    public int hotelId;
    public long createdAt;
    public boolean read, notified;
}
