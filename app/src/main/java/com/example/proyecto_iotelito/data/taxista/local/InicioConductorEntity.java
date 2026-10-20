package com.example.proyecto_iotelito.data.taxista.local;
import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName="conductor_inicio")
public class InicioConductorEntity {
    @PrimaryKey @NonNull public String cuenta="";
}
