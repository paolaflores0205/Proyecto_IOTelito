package com.example.proyecto_iotelito.data.superadmin.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** Base propia del rol para no alterar la base ni las pantallas del cliente. */
@Database(entities = {AdminHotelEntity.class, AdminUserEntity.class,
        AdminLogEntity.class, AdminAlertEntity.class, ReportDemoBookingEntity.class}, version = 5, exportSchema = false)
public abstract class SuperadminDatabase extends RoomDatabase {
    private static volatile SuperadminDatabase instance;
    public abstract SuperadminDao dao();
    public static final androidx.room.migration.Migration MIGRATION_1_2 =
            new androidx.room.migration.Migration(1, 2) {
                @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                    db.execSQL("ALTER TABLE admin_hotels ADD COLUMN photosJson TEXT");
                }
            };

    public static final androidx.room.migration.Migration MIGRATION_2_3 =
            new androidx.room.migration.Migration(2, 3) {
                @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                    db.execSQL("CREATE TABLE IF NOT EXISTS report_demo_bookings (id INTEGER NOT NULL PRIMARY KEY, "
                            + "hotelId INTEGER NOT NULL, arrival TEXT NOT NULL, departure TEXT NOT NULL, "
                            + "roomCodes TEXT NOT NULL, state TEXT NOT NULL, code TEXT NOT NULL, total REAL NOT NULL)");
                }
            };

    public static final androidx.room.migration.Migration MIGRATION_3_4 =
            new androidx.room.migration.Migration(3, 4) {
                @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                    db.execSQL("ALTER TABLE admin_hotels ADD COLUMN galleryInitialized INTEGER NOT NULL DEFAULT 0");
                }
            };
    public static final androidx.room.migration.Migration MIGRATION_4_5 =
            new androidx.room.migration.Migration(4, 5) {
                @Override public void migrate(androidx.sqlite.db.SupportSQLiteDatabase db) {
                    db.execSQL("ALTER TABLE admin_hotels ADD COLUMN attractions TEXT");
                }
            };

    public static SuperadminDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (SuperadminDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            SuperadminDatabase.class, "iotelito_superadmin.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build();
                }
            }
        }
        return instance;
    }
}
