package com.example.proyecto_iotelito.data.superadmin.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** Base propia del rol para no alterar la base ni las pantallas del cliente. */
@Database(entities = {AdminHotelEntity.class, AdminUserEntity.class,
        AdminLogEntity.class, AdminAlertEntity.class}, version = 1, exportSchema = false)
public abstract class SuperadminDatabase extends RoomDatabase {
    private static volatile SuperadminDatabase instance;
    public abstract SuperadminDao dao();

    public static SuperadminDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (SuperadminDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            SuperadminDatabase.class, "iotelito_superadmin.db").build();
                }
            }
        }
        return instance;
    }
}
