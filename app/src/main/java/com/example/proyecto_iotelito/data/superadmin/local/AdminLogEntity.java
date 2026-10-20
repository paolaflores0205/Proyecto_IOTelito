package com.example.proyecto_iotelito.data.superadmin.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.proyecto_iotelito.model.superadmin.ActivityLog;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(tableName = "admin_events")
public class AdminLogEntity {
    @PrimaryKey(autoGenerate = true) public long id;
    public String type, title, detail;
    public long createdAt;

    public ActivityLog toModel() {
        String time = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                .format(new Date(createdAt));
        return new ActivityLog(type, title, detail, time);
    }
}
