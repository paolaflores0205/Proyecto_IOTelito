package com.example.proyecto_iotelito.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import androidx.annotation.NonNull;

@Database(entities = {ReservaEntity.class, NotificacionEntity.class}, version = 3, exportSchema = false)
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

    // Los avisos heredados carecen de propietario: se conservan con UID vacío,
    // fuera de todas las bandejas autenticadas. Nunca se adjudican a quien inicia sesión.
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `notificaciones_por_cuenta` (`ownerUid` TEXT NOT NULL, "
                    + "`id` INTEGER NOT NULL, `tipo` TEXT, `titulo` TEXT, `creadaEn` INTEGER NOT NULL, "
                    + "`leida` INTEGER NOT NULL, `reservaId` INTEGER NOT NULL, PRIMARY KEY(`ownerUid`, `id`))");
            database.execSQL("INSERT INTO notificaciones_por_cuenta SELECT '', id, tipo, titulo, creadaEn, leida, reservaId FROM notificaciones_cliente");
            database.execSQL("DROP TABLE notificaciones_cliente");
            database.execSQL("ALTER TABLE notificaciones_por_cuenta RENAME TO notificaciones_cliente");
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
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return instance;
    }
}
