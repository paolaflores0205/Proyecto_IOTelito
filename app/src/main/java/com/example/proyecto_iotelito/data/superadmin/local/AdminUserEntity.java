package com.example.proyecto_iotelito.data.superadmin.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.proyecto_iotelito.model.superadmin.AdminUser;

@Entity(tableName = "admin_users")
public class AdminUserEntity {
    @PrimaryKey public int id;
    public String name, email, document, phone, role, registeredAt, lastAccess;
    public boolean active;

    public static AdminUserEntity from(AdminUser user) {
        AdminUserEntity row = new AdminUserEntity();
        row.id = user.id;
        row.name = user.name;
        row.email = user.email;
        row.document = user.document;
        row.phone = user.phone;
        row.role = user.role;
        row.registeredAt = user.registeredAt;
        row.lastAccess = user.lastAccess;
        row.active = user.active;
        return row;
    }

    public AdminUser toModel() {
        return new AdminUser(id, name, email, document, phone, role, registeredAt, lastAccess, active);
    }
}
