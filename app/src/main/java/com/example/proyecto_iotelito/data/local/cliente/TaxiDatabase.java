package com.example.proyecto_iotelito.data.local.cliente;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {TaxiEntity.class}, version = 2, exportSchema = false)
public abstract class TaxiDatabase extends RoomDatabase {
    private static volatile TaxiDatabase instance;

    public abstract TaxiDao taxiDao();

    public static final androidx.room.migration.Migration MIGRATION_1_2=new androidx.room.migration.Migration(1,2){
        @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db){
            db.execSQL("ALTER TABLE servicios_taxi ADD COLUMN hotelId INTEGER");
            for(String field:new String[]{"hotelCity","airportCode","airportName","airportCity"})
                db.execSQL("ALTER TABLE servicios_taxi ADD COLUMN "+field+" TEXT");
        }
    };

    public static TaxiDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (TaxiDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            TaxiDatabase.class, "taxis_cliente.db").addMigrations(MIGRATION_1_2).build();
                }
            }
        }
        return instance;
    }
}
