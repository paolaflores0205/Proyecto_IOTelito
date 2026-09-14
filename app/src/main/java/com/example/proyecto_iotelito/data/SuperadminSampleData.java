package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.superadmin.ActivityLog;
import com.example.proyecto_iotelito.model.superadmin.AdminUser;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class SuperadminSampleData {
    private static final List<AdminUser> USERS = new ArrayList<>(Arrays.asList(
            new AdminUser(1, "María García López", "maria.garcia@gmail.com", "DNI 74859621", "+51 987 654 321", "Cliente", "15/03/2024", "Hoy, 09:32", true),
            new AdminUser(2, "Carlos Mendoza Quispe", "carlos.mendoza@iotelito.pe", "DNI 43698215", "+51 966 318 240", "Administrador", "02/01/2024", "Ayer, 18:05", true),
            new AdminUser(3, "Jorge Ramírez Soto", "jorge.ramirez@gmail.com", "DNI 70125463", "+51 955 127 880", "Taxista", "21/05/2024", "10/09/2026", true),
            new AdminUser(4, "Lucía Fernández Rojas", "lucia.fernandez@gmail.com", "DNI 65987412", "+51 944 506 127", "Cliente", "08/08/2024", "Hace 64 días", false)
    ));

    private static final List<ManagedHotel> HOTELS = Arrays.asList(
            new ManagedHotel(1, "Hotel Sol de Miraflores", "Av. Larco 345, Miraflores, Lima", "Miraflores", "Juan Carlos Huamán", 24, 156, R.drawable.superadmin_hotel_sol, true),
            new ManagedHotel(2, "Barranco Art Boutique", "Jr. Centenario 102, Barranco, Lima", "Barranco", "Sin asignar", 18, 92, R.drawable.superadmin_hotel_barranco, true)
    );

    private static final List<ActivityLog> LOGS = Arrays.asList(
            new ActivityLog("Sistema", "Inicio de sesión exitoso", "Superadmin ingresó desde Lima", "Hoy, 10:42"),
            new ActivityLog("Hotel", "Hotel registrado", "Hotel Sol de Miraflores fue agregado", "Hoy, 09:15"),
            new ActivityLog("Usuario", "Perfil actualizado", "Carlos Mendoza modificó sus datos", "Ayer, 18:05"),
            new ActivityLog("Alerta", "Usuario desactivado", "Cuenta inactiva por más de 60 días", "Ayer, 16:30"),
            new ActivityLog("Sistema", "Sincronización IoT", "38 hoteles reportaron correctamente", "12/09/2026"),
            new ActivityLog("Hotel", "Administrador asignado", "Juan Carlos Huamán recibió acceso", "11/09/2026")
    );

    private SuperadminSampleData() { }

    public static List<AdminUser> users() {
        return USERS;
    }

    public static AdminUser user(int id) {
        for (AdminUser user : USERS) if (user.id == id) return user;
        return USERS.get(0);
    }

    public static List<ManagedHotel> hotels() {
        return HOTELS;
    }

    public static ManagedHotel hotel(int id) {
        for (ManagedHotel hotel : HOTELS) if (hotel.id == id) return hotel;
        return HOTELS.get(0);
    }

    public static List<ActivityLog> logs() {
        return LOGS;
    }
}
