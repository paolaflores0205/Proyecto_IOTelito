package com.example.proyecto_iotelito.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import androidx.annotation.NonNull;

@Database(entities = {ReservaEntity.class, NotificacionEntity.class}, version = 2, exportSchema = false)
public abstract class IoTelitoDatabase extends RoomDatabase {

    private static volatile IoTelitoDatabase instance;

    public abstract ReservaDao reservaDao();
    public abstract NotificacionDao notificacionDao();

    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `notificaciones_cliente` "
                    + "(`id` INTEGER NOT NULL, `tipo` TEXT, `titulo` TEXT, "
                    + "`creadaEn` INTEGER NOT NULL, `leida` INTEGER NOT NULL, "
                    + "`reservaId` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        }
    };

    public static IoTelitoDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (IoTelitoDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    IoTelitoDatabase.class,
                                    "iotelito_local.db")
                            .addMigrations(MIGRATION_1_2)
                            // El dataset del curso es pequeño y las pantallas actuales son síncronas.
                            // En la integración con Firebase estas consultas deben moverse a un Executor.
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return instance;
    }
}
