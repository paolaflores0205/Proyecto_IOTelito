package com.example.proyecto_iotelito.data.taxista.local;
import android.content.Context;
import androidx.room.*;
@Database(entities={ServicioConductorEntity.class,AvisoConductorEntity.class,MensajeConductorEntity.class,InicioConductorEntity.class},version=1,exportSchema=false)
public abstract class ConductorDatabase extends RoomDatabase {
    private static volatile ConductorDatabase instance;
    public abstract ConductorDao dao();
    public static ConductorDatabase get(Context context) {
        if(instance==null) synchronized(ConductorDatabase.class) {
            if(instance==null) instance=Room.databaseBuilder(context.getApplicationContext(),ConductorDatabase.class,"iotelito_taxista.db").build();
        }
        return instance;
    }
}
