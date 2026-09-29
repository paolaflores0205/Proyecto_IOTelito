package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.taxista.ServicioHistorial;
import com.example.proyecto_iotelito.model.taxista.SolicitudServicio;
import com.example.proyecto_iotelito.ui.taxista.EstadoServicio;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Data set estático del módulo Taxista (rol TAXISTA), espejo de
 * {@link HotelAdminSampleData}. Se usa mientras no exista la persistencia
 * (Lab 6). La lista de solicitudes es mutable: al aceptar un servicio deja de
 * estar disponible, como exige el plan de proyecto.
 */
public final class TaxistaSampleData {

    private static final String AEROPUERTO = "Aeropuerto Internacional Jorge Chávez";

    private static final List<SolicitudServicio> SOLICITUDES = new ArrayList<>(Arrays.asList(
            new SolicitudServicio(1, 4, "Hotel Miraflores Park", AEROPUERTO,
                    "02 ago 2026, 14:00", "Ana García", "AG", "14 min · 6.2 km", 45.50, "18.2 km", "25 min"),
            new SolicitudServicio(2, 20, "Palacio del Inka Cusco (Lima Branch)", AEROPUERTO,
                    "03 ago 2026, 15:30", "Carlos Mendoza", "CM", "22 min · 9.8 km", 62.00, "22.5 km", "34 min"),
            new SolicitudServicio(3, 10, "Dazzler Miraflores", AEROPUERTO,
                    "04 ago 2026, 09:15", "Rodrigo Salas", "RS", "11 min · 4.5 km", 38.00, "17.8 km", "24 min"),
            new SolicitudServicio(4, 35, "Barranco Art Boutique", AEROPUERTO,
                    "05 ago 2026, 11:00", "Valentina Ríos", "VR", "38 min · 21 km", 55.00, "24 km", "38 min"),
            new SolicitudServicio(5, 8, "Casa Andina Premium Miraflores", AEROPUERTO,
                    "06 ago 2026, 06:45", "Lucía Fernández", "LF", "9 min · 3.8 km", 32.00, "16.5 km", "23 min"),
            new SolicitudServicio(6, 15, "Hotel B Barranco", AEROPUERTO,
                    "06 ago 2026, 18:20", "Mateo Quispe", "MQ", "17 min · 7.1 km", 48.00, "23 km", "36 min")));

    private static final List<ServicioHistorial> HISTORIAL = Arrays.asList(
            new ServicioHistorial(1, "14:10", EstadoServicio.FINALIZADO,
                    "Hotel Miraflores Park", "Aeropuerto Jorge Chávez",
                    "María García", "Toyota Corolla", "18.2 km", "25 min", null, 45.50),
            new ServicioHistorial(2, "10:45", EstadoServicio.FINALIZADO,
                    "Dazzler Miraflores", "Aeropuerto Jorge Chávez",
                    "Juan Pérez", "Toyota Corolla", "15 km", "32 min", null, 38.00),
            new ServicioHistorial(3, "ayer, 19:30", EstadoServicio.CANCELADO,
                    "Casa Andina Premium", "Aeropuerto Jorge Chávez",
                    "Lucía Fernández", "Toyota Corolla", null, null, "Cancelado por el pasajero", 0),
            new ServicioHistorial(4, "ayer, 08:15", EstadoServicio.FINALIZADO,
                    "Barranco Art Boutique", "Aeropuerto Jorge Chávez",
                    "Sofía Paredes", "Toyota Corolla", "21 km", "35 min", null, 52.00),
            new ServicioHistorial(5, "ayer, 06:40", EstadoServicio.FINALIZADO,
                    "Hotel B Barranco", "Aeropuerto Jorge Chávez",
                    "Diego Torres", "Toyota Corolla", "19.5 km", "28 min", null, 60.00),
            new ServicioHistorial(6, "hace 2 días, 21:05", EstadoServicio.CANCELADO,
                    "Palacio del Inka Cusco (Lima Branch)", "Aeropuerto Jorge Chávez",
                    "Renzo Vega", "Toyota Corolla", null, null, "Cancelado por el conductor", 0),
            new ServicioHistorial(7, "hace 2 días, 13:20", EstadoServicio.FINALIZADO,
                    "Country Club Lima Hotel", "Aeropuerto Jorge Chávez",
                    "Camila Rojas", "Toyota Corolla", "22 km", "30 min", null, 50.00));

    /** Servicio que se muestra si se abre el flujo sin haber aceptado ninguna solicitud. */
    private static final SolicitudServicio SERVICIO_DEMO = new SolicitudServicio(0, 4,
            "Hotel Miraflores Park", AEROPUERTO, "02 ago 2026, 14:00", "Ana García", "AG",
            "14 min · 6.2 km", 45.50, "18.2 km", "25 min");

    /** Solicitud que el taxista aceptó y está atendiendo; null si no tiene ningún servicio en curso. */
    private static SolicitudServicio servicioActivo;

    private TaxistaSampleData() {
    }

    /** Solicitudes aún disponibles (sin taxista asignado). */
    public static List<SolicitudServicio> solicitudes() {
        return SOLICITUDES;
    }

    public static SolicitudServicio solicitudPorId(int id) {
        for (SolicitudServicio solicitud : SOLICITUDES) {
            if (solicitud.id == id) {
                return solicitud;
            }
        }
        return null;
    }

    /**
     * Servicio en curso, compartido por todas las pantallas del flujo (asignado, camino al hotel,
     * traslado, QR y resumen final). Si no hay ninguno devuelve un servicio de ejemplo.
     */
    public static SolicitudServicio servicioActivo() {
        return servicioActivo != null ? servicioActivo : SERVICIO_DEMO;
    }

    /** El servicio terminó: el taxista queda libre para aceptar otro. */
    public static void finalizarServicioActivo() {
        servicioActivo = null;
    }

    /** El taxista acepta el pedido: queda asignado (servicio activo) y deja de estar disponible para los demás. */
    public static void aceptarSolicitud(int id) {
        Iterator<SolicitudServicio> it = SOLICITUDES.iterator();
        while (it.hasNext()) {
            SolicitudServicio solicitud = it.next();
            if (solicitud.id == id) {
                it.remove();
                servicioActivo = solicitud;
                return;
            }
        }
    }

    public static ServicioHistorial historialPorId(int id) {
        for (ServicioHistorial servicio : HISTORIAL) {
            if (servicio.id == id) {
                return servicio;
            }
        }
        return null;
    }

    /** Traslados pasados, del más reciente al más antiguo. */
    public static List<ServicioHistorial> historial() {
        return HISTORIAL;
    }
}
