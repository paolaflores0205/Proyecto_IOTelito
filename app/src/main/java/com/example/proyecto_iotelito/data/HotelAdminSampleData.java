package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.model.Mensaje;
import com.example.proyecto_iotelito.model.hoteladmin.Conversacion;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;
import com.example.proyecto_iotelito.model.hoteladmin.ItemConsumo;
import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;
import com.example.proyecto_iotelito.model.hoteladmin.Servicio;
import com.example.proyecto_iotelito.model.hoteladmin.ServicioTaxi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Data set estático del módulo Administrador de hotel (rol HOTEL_ADMIN), espejo de
 * {@link SuperadminSampleData}. Se usa mientras no exista la persistencia
 * NoSQL/Firebase (Lab 6). Las listas son mutables para que los formularios puedan
 * crear/editar/eliminar en memoria y reflejarse al volver (patrón onResume).
 *
 * El hotel administrado es el "Hotel Miraflores Park" (id 1). Las fotografías
 * reutilizan drawables existentes como placeholders realistas.
 */
public final class HotelAdminSampleData {

    public static final int HOTEL_ID = 1;

    /** Nombre del administrador autenticado (mostrado en el saludo del dashboard). */
    public static String adminNombre = "Admin Carlos";

    // ---- Datos editables del hotel (pantallas Datos del hotel / Fotos) ----
    public static String hotelNombre = "Hotel Miraflores Park";
    public static String hotelDireccion = "Av. Malecón de la Reserva 1037, Miraflores, Lima";
    public static String hotelDescripcion =
            "Ubicado frente al imponente Océano Pacífico, este hotel combina elegancia "
                    + "clásica con conectividad inteligente de última generación.";
    public static String hotelAtracciones =
            "Parque Kennedy (1.2 km) · Huaca Pucllana (1.5 km) · Malecón de Miraflores (0.1 km)";
    public static String hotelTelefono = "+51 1 610 4000";
    public static String hotelEmail = "reservas@mirafloraspark.pe";

    /** Fotos del hotel (mín. 4). El índice 0 es la foto principal. */
    public static final List<Integer> FOTOS = new ArrayList<>(Arrays.asList(
            R.drawable.superadmin_hotel_hero,
            R.drawable.superadmin_hotel_sol,
            R.drawable.superadmin_hotel_barranco,
            R.drawable.superadmin_hotel_hero
    ));

    // ---- Habitaciones ----
    private static final List<Habitacion> HABITACIONES = new ArrayList<>(Arrays.asList(
            new Habitacion(1, HOTEL_ID, "Suite Ejecutiva", "Hab. 402", 45, 2, 1, 450.0,
                    "Cama King Size, vista al mar, minibar inteligente y Smart TV 55\".",
                    R.drawable.superadmin_hotel_hero, true),
            new Habitacion(2, HOTEL_ID, "Habitación Doble", "Hab. 305", 30, 2, 0, 320.0,
                    "Dos camas queen, escritorio de trabajo y Wi-Fi ultra-veloz.",
                    R.drawable.superadmin_hotel_sol, false),
            new Habitacion(3, HOTEL_ID, "Habitación Simple", "Hab. 210", 22, 1, 0, 190.0,
                    "Cama individual, ideal para viajeros de negocios.",
                    R.drawable.superadmin_hotel_barranco, true),
            new Habitacion(4, HOTEL_ID, "Suite Deluxe", "Hab. 501", 55, 2, 2, 620.0,
                    "Suite familiar con sala independiente y jacuzzi.",
                    R.drawable.superadmin_hotel_hero, true),
            new Habitacion(5, HOTEL_ID, "Habitación Superior", "Hab. 308", 35, 2, 1, 380.0,
                    "Vista a la ciudad, calefacción inteligente y balcón.",
                    R.drawable.superadmin_hotel_sol, false)
    ));

    // ---- Servicios adicionales ----
    private static final List<Servicio> SERVICIOS = new ArrayList<>(Arrays.asList(
            new Servicio(1, HOTEL_ID, "Desayuno Buffet",
                    "Variedad de comida internacional y peruana, de 6:00 a 10:00 a.m.",
                    45.0, R.drawable.superadmin_hotel_sol, true),
            new Servicio(2, HOTEL_ID, "Lavandería Express",
                    "Lavado y planchado en menos de 4 horas.",
                    35.0, R.drawable.superadmin_hotel_barranco, true),
            new Servicio(3, HOTEL_ID, "Traslado Aeropuerto",
                    "Servicio privado seguro hacia/desde el aeropuerto.",
                    80.0, R.drawable.superadmin_hotel_hero, true),
            new Servicio(4, HOTEL_ID, "Spa & Masajes",
                    "Sesiones terapéuticas y de relajación corporal.",
                    120.0, R.drawable.superadmin_hotel_sol, false)
    ));

    // ---- Reservas ----
    private static final List<ReservaAdmin> RESERVAS = new ArrayList<>(Arrays.asList(
            new ReservaAdmin(101, HOTEL_ID, "Alejandro Quispe Mendoza", "DNI 45678910",
                    "Suite Ejecutiva · Hab. 402", "15 – 18 set", 3, 1350.0, true,
                    ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE, new ArrayList<>(Arrays.asList(
                    new ItemConsumo("Alojamiento (3 noches)", 1350.0),
                    new ItemConsumo("Desayuno Buffet x3", 135.0),
                    new ItemConsumo("Lavandería Express", 35.0),
                    new ItemConsumo("Traslado Aeropuerto", 80.0)))),
            new ReservaAdmin(102, HOTEL_ID, "Sofía Alva Rodríguez", "DNI 70854123",
                    "Habitación Doble · Hab. 305", "14 – 17 set", 3, 960.0, false,
                    ReservaAdmin.EstadoCheckout.HOSPEDADO, new ArrayList<>(Arrays.asList(
                    new ItemConsumo("Alojamiento (3 noches)", 960.0),
                    new ItemConsumo("Desayuno Buffet x2", 90.0)))),
            new ReservaAdmin(103, HOTEL_ID, "Carlos Ruiz Ramos", "DNI 41235876",
                    "Suite Deluxe · Hab. 501", "20 – 23 set", 3, 1860.0, true,
                    ReservaAdmin.EstadoCheckout.PROXIMA, new ArrayList<>(Collections.singletonList(
                    new ItemConsumo("Alojamiento (3 noches)", 1860.0)))),
            new ReservaAdmin(104, HOTEL_ID, "Mariana Huertas Díaz", "DNI 09876543",
                    "Habitación Superior · Hab. 308", "10 – 12 set", 2, 760.0, false,
                    ReservaAdmin.EstadoCheckout.FINALIZADA, new ArrayList<>(Arrays.asList(
                    new ItemConsumo("Alojamiento (2 noches)", 760.0),
                    new ItemConsumo("Spa & Masajes", 120.0))))
    ));

    // ---- Taxi activo ----
    private static final ServicioTaxi TAXI_ACTIVO = new ServicioTaxi(
            "Alejandro Quispe Mendoza", "Suite Ejecutiva · Hab. 402",
            "Aeropuerto Internacional Jorge Chávez", "Mateo Quispe Huamán",
            "Toyota Corolla · Gris", "ABC-123", "+51 987 112 334", 4.8,
            ServicioTaxi.Estado.EN_CAMINO);

    // ---- Conversaciones ----
    private static final List<Conversacion> CONVERSACIONES = new ArrayList<>(Arrays.asList(
            new Conversacion(1, "María López", "Hab. 204",
                    "Buenas tardes, ¿a qué hora es el desayuno?", "10:32 a.m.", "M", true, 2),
            new Conversacion(2, "Luis Lingán", "Hab. 210",
                    "Perfecto, muchas gracias.", "09:15 a.m.", "L", true, 0),
            new Conversacion(3, "Mariana Huertas", "Hab. 308",
                    "Quiero confirmar mi checkout de mañana.", "Ayer", "M", false, 0),
            new Conversacion(4, "Matthew Salvador", "Hab. 402",
                    "El aire acondicionado no está enfriando.", "Ayer", "M", true, 1)
    ));

    private HotelAdminSampleData() {
    }

    // ---------------- Accesores ----------------

    public static List<Habitacion> habitaciones() {
        return HABITACIONES;
    }

    public static Habitacion habitacion(int id) {
        for (Habitacion h : HABITACIONES) if (h.id == id) return h;
        return HABITACIONES.get(0);
    }

    public static int nextHabitacionId() {
        int max = 0;
        for (Habitacion h : HABITACIONES) max = Math.max(max, h.id);
        return max + 1;
    }

    public static List<Servicio> servicios() {
        return SERVICIOS;
    }

    public static Servicio servicio(int id) {
        for (Servicio s : SERVICIOS) if (s.id == id) return s;
        return SERVICIOS.get(0);
    }

    public static int nextServicioId() {
        int max = 0;
        for (Servicio s : SERVICIOS) max = Math.max(max, s.id);
        return max + 1;
    }

    public static List<ReservaAdmin> reservas() {
        return RESERVAS;
    }

    public static ReservaAdmin reserva(int id) {
        for (ReservaAdmin r : RESERVAS) if (r.id == id) return r;
        return RESERVAS.get(0);
    }

    public static ServicioTaxi taxiActivo() {
        return TAXI_ACTIVO;
    }

    public static List<Conversacion> conversaciones() {
        return CONVERSACIONES;
    }

    public static Conversacion conversacion(int id) {
        for (Conversacion c : CONVERSACIONES) if (c.id == id) return c;
        return CONVERSACIONES.get(0);
    }

    /** Conversación estática de ejemplo con un huésped. */
    public static List<Mensaje> chatDe(int conversacionId) {
        Conversacion c = conversacion(conversacionId);
        List<Mensaje> mensajes = new ArrayList<>();
        mensajes.add(new Mensaje("Hola, buenas tardes.", true, "10:20 a.m."));
        mensajes.add(new Mensaje("¡Hola " + c.clienteNombre + "! Bienvenido al "
                + hotelNombre + ". ¿En qué podemos ayudarle?", false, "10:22 a.m."));
        mensajes.add(new Mensaje("Quería consultar el horario del desayuno.", true, "10:31 a.m."));
        mensajes.add(new Mensaje("El desayuno buffet se sirve de 6:00 a 10:00 a.m. en el "
                + "restaurante del primer piso.", false, "10:33 a.m."));
        return mensajes;
    }

    // ---------------- Resumen del día (dashboard) ----------------

    public static final class ResumenHoy {
        public final int ocupadas;
        public final int disponibles;
        public final int salidasHoy;
        public final double ingresosHoy;

        ResumenHoy(int ocupadas, int disponibles, int salidasHoy, double ingresosHoy) {
            this.ocupadas = ocupadas;
            this.disponibles = disponibles;
            this.salidasHoy = salidasHoy;
            this.ingresosHoy = ingresosHoy;
        }
    }

    public static ResumenHoy resumenHoy() {
        return new ResumenHoy(12, 8, 3, 4520.0);
    }

    // ---------------- Reportes ----------------

    public static final class Reporte {
        public final double ingresos;
        public final int reservas;
        public final int ocupacionPct;
        public final int serviciosVendidos;
        /** Ingresos por semana del mes (para el gráfico de barras). */
        public final int[] ingresosSemanales;
        /** Ingresos por servicio adicional, ordenados de menor a mayor (spec). */
        public final List<ItemConsumo> ingresosPorServicio;

        Reporte(double ingresos, int reservas, int ocupacionPct, int serviciosVendidos,
                int[] ingresosSemanales, List<ItemConsumo> ingresosPorServicio) {
            this.ingresos = ingresos;
            this.reservas = reservas;
            this.ocupacionPct = ocupacionPct;
            this.serviciosVendidos = serviciosVendidos;
            this.ingresosSemanales = ingresosSemanales;
            this.ingresosPorServicio = ingresosPorServicio;
        }
    }

    public static Reporte reporte() {
        List<ItemConsumo> porServicio = new ArrayList<>(Arrays.asList(
                new ItemConsumo("Traslado Aeropuerto", 2880.0),
                new ItemConsumo("Desayuno Buffet", 4320.0),
                new ItemConsumo("Lavandería Express", 1240.0),
                new ItemConsumo("Spa & Masajes", 3600.0)
        ));
        // Ordenados de menor a mayor según el monto total generado (requisito del plan).
        Collections.sort(porServicio, new Comparator<ItemConsumo>() {
            @Override
            public int compare(ItemConsumo a, ItemConsumo b) {
                return Double.compare(a.monto, b.monto);
            }
        });
        return new Reporte(45280.0, 38, 78, 124,
                new int[]{9800, 12400, 10600, 12480}, porServicio);
    }
}
