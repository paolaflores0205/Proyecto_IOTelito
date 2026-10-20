package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.Hotel;
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
            new AdminUser(4, "Lucía Fernández Rojas", "lucia.fernandez@gmail.com", "DNI 65987412", "+51 944 506 127", "Cliente", "08/08/2024", "Hace 64 días", false),
            new AdminUser(5, "Juan Carlos Huamán", "juan.huaman@example.com", "DNI 45123005", "+51 900 100 005", "Administrador", "12/01/2026", "Hoy, 08:15", true),
            new AdminUser(6, "Valeria Torres Medina", "valeria.torres@example.com", "DNI 45123006", "+51 900 100 006", "Administrador", "18/02/2026", "Hoy, 10:20", true),
            new AdminUser(7, "Diego Salazar Ramos", "diego.salazar@example.com", "DNI 45123007", "+51 900 100 007", "Cliente", "03/03/2026", "Ayer, 14:10", true),
            new AdminUser(8, "Camila Vargas Ruiz", "camila.vargas@example.com", "DNI 45123008", "+51 900 100 008", "Cliente", "20/03/2026", "Hace 70 días", false),
            new AdminUser(9, "Luis Paredes Vega", "luis.paredes@example.com", "DNI 45123009", "+51 900 100 009", "Taxista", "09/04/2026", "Hoy, 07:40", true),
            new AdminUser(10, "Rosa Aguilar Díaz", "rosa.aguilar@example.com", "DNI 45123010", "+51 900 100 010", "Administrador", "15/04/2026", "Ayer, 17:30", true),
            new AdminUser(11, "Pedro Castillo León", "pedro.castillo@example.com", "DNI 45123011", "+51 900 100 011", "Taxista", "05/05/2026", "Hace 68 días", false),
            new AdminUser(12, "Sofía Navarro Pérez", "sofia.navarro@example.com", "DNI 45123012", "+51 900 100 012", "Cliente", "12/05/2026", "Hoy, 11:05", true),
            new AdminUser(13, "Andrés Rojas Silva", "andres.rojas@example.com", "DNI 45123013", "+51 900 100 013", "Superadmin", "01/06/2026", "Hoy, 09:00", true),
            new AdminUser(14, "Elena Flores Sánchez", "elena.flores@example.com", "DNI 45123014", "+51 900 100 014", "Administrador", "10/06/2026", "Hace 75 días", false),
            new AdminUser(15, "Miguel Herrera López", "miguel.herrera@example.com", "DNI 45123015", "+51 900 100 015", "Cliente", "24/07/2026", "Ayer, 20:45", true),
            new AdminUser(16, "Patricia Soto Gómez", "patricia.soto@example.com", "DNI 45123016", "+51 900 100 016", "Superadmin", "02/08/2026", "Hoy, 08:50", true)
    ));

    private static final List<ManagedHotel> HOTELS = crearHoteles();

    private static List<ManagedHotel> crearHoteles() {
        String[] distritos = {"Miraflores", "Centro Histórico", "Yanahuara", "Miraflores",
                "Barranco", "San Isidro", "Barranco", "Surco", "Centro Histórico",
                "Cercado", "Centro Histórico", "Paracas"};
        String[] administradores = {"Carlos Mendoza Quispe", "Valeria Torres Medina",
                "Rosa Aguilar Díaz", "Juan Carlos Huamán", "Sin asignar",
                "Carlos Mendoza Quispe", "Sin asignar", "Juan Carlos Huamán",
                "Valeria Torres Medina", "Rosa Aguilar Díaz", "Sin asignar", "Juan Carlos Huamán"};
        int[] habitaciones = {42, 36, 30, 24, 18, 32, 16, 28, 22, 26, 20, 38};
        int[] reservas = {156, 132, 98, 84, 65, 110, 48, 76, 92, 72, 54, 124};
        List<ManagedHotel> hoteles = new ArrayList<>();
        for (Hotel hotel : SampleData.HOTELS) {
            int posicion = hotel.id - 1;
            hoteles.add(new ManagedHotel(hotel.id, hotel.name, hotel.address,
                    distritos[posicion], administradores[posicion], habitaciones[posicion],
                    reservas[posicion], HotelMedia.hotelImage(hotel.id), true));
        }
        return hoteles;
    }

    private static final List<ActivityLog> LOGS = Arrays.asList(
            new ActivityLog("Sistema", "Inicio de sesión exitoso", "Superadmin ingresó desde Lima", "Hoy, 10:42"),
            new ActivityLog("Hotel", "Hotel registrado", "Hotel Sol de Miraflores fue agregado", "Hoy, 09:15"),
            new ActivityLog("Usuario", "Perfil actualizado", "Carlos Mendoza modificó sus datos", "Ayer, 18:05"),
            new ActivityLog("Alerta", "Usuario desactivado", "Cuenta inactiva por más de 60 días", "Ayer, 16:30"),
            new ActivityLog("Sistema", "Sincronización IoT", HOTELS.size() + " hoteles reportaron correctamente", "12/09/2026"),
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
