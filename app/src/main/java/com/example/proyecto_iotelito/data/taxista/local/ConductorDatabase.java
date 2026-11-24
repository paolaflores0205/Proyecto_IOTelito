package com.example.proyecto_iotelito.data.taxista.local;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.room.*;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
@Database(entities={ServicioConductorEntity.class,AvisoConductorEntity.class,MensajeConductorEntity.class,InicioConductorEntity.class},version=2,exportSchema=false)
public abstract class ConductorDatabase extends RoomDatabase {
    private static volatile ConductorDatabase instance;
    public abstract ConductorDao dao();
    /** v2: hotel y aeropuerto del viaje, para el mapa con ruta. */
    static final Migration MIGRACION_1_2=new Migration(1,2){
        @Override public void migrate(@NonNull SupportSQLiteDatabase db){
            db.execSQL("ALTER TABLE conductor_servicios ADD COLUMN hotelId INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE conductor_servicios ADD COLUMN aeropuertoCodigo TEXT");
        }
    };
    public static ConductorDatabase get(Context context) {
        if(instance==null) synchronized(ConductorDatabase.class) {
            if(instance==null) instance=Room.databaseBuilder(context.getApplicationContext(),ConductorDatabase.class,"iotelito_taxista.db").addMigrations(MIGRACION_1_2).build();
        }
        return instance;
    }
}
