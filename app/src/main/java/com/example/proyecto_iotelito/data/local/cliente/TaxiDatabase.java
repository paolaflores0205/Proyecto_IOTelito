package com.example.proyecto_iotelito.data.local.cliente;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {TaxiEntity.class}, version = 1, exportSchema = false)
public abstract class TaxiDatabase extends RoomDatabase {
    private static volatile TaxiDatabase instance;

    public abstract TaxiDao taxiDao();

    public static TaxiDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (TaxiDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            TaxiDatabase.class, "taxis_cliente.db").build();
                }
            }
        }
        return instance;
    }
}
