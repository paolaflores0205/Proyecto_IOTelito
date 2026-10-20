package com.example.proyecto_iotelito.data.taxista.local;
import androidx.annotation.NonNull;
import androidx.room.Entity;
@Entity(tableName="conductor_avisos",primaryKeys={"cuenta","clave"})
public class AvisoConductorEntity {
    @NonNull public String cuenta="", clave="";
    public int servicio;
    public String tipo="", titulo="", texto="";
    public long fecha;
    public boolean leido, publicado;
}
