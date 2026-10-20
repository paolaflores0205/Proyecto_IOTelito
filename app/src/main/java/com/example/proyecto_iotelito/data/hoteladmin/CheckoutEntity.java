package com.example.proyecto_iotelito.data.hoteladmin;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "checkouts_admin")
public class CheckoutEntity {
    @PrimaryKey public int reservaId;
    public String estado;
    public String consumosJson;
}
