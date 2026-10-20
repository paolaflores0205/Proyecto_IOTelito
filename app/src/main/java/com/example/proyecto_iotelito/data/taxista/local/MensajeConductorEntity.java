package com.example.proyecto_iotelito.data.taxista.local;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName="conductor_mensajes")
public class MensajeConductorEntity {
    @PrimaryKey(autoGenerate=true) public long id;
    @NonNull public String cuenta="";
    public int servicio;
    public String texto="";
    public boolean mio;
    public long fecha;
}
