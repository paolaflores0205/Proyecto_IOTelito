package com.example.proyecto_iotelito.model.superadmin;

public class ActivityLog {
    public final String type;
    public final String title;
    public final String detail;
    public final String time;

    public ActivityLog(String type, String title, String detail, String time) {
        this.type = type;
        this.title = title;
        this.detail = detail;
        this.time = time;
    }
}
