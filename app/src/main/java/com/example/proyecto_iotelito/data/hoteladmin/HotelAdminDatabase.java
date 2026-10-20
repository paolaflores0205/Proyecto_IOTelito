package com.example.proyecto_iotelito.data.hoteladmin;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {HabitacionEntity.class, ServicioEntity.class, CheckoutEntity.class,
        MensajeAdminEntity.class},
        version = 1, exportSchema = false)
public abstract class HotelAdminDatabase extends RoomDatabase {
    private static volatile HotelAdminDatabase instance;

    public abstract HotelAdminDao dao();

    public static HotelAdminDatabase get(Context context) {
        if (instance == null) {
            synchronized (HotelAdminDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            HotelAdminDatabase.class, "hoteladmin.db").build();
                }
            }
        }
        return instance;
    }
}
