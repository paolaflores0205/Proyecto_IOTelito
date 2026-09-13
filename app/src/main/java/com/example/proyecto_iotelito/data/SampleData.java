package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.Attraction;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Mensaje;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.UserProfile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Data set estático usado mientras no se conecta la persistencia NoSQL.
 * Los valores replican el contenido de los mockups de Figma.
 */
public final class SampleData {

    private SampleData() {
    }

    public static final List<Hotel> HOTELS = Collections.unmodifiableList(new ArrayList<Hotel>() {{
        add(new Hotel(
                1, "Lima", "Hotel Miraflores Park",
                "Av. Malecón de la Reserva 1037, Miraflores, Lima",
                4.7, 234,
                "Ubicado frente al imponente Océano Pacífico, este hotel combina elegancia " +
                        "clásica con conectividad inteligente de última generación para garantizar " +
                        "la mejor experiencia de descanso en Lima.",
                450.0,
                new String[]{"Wi-Fi ultra-veloz", "Desayuno incluido", "Piscina temperada"},
                new Attraction[]{
                        new Attraction("Huaca Pucllana", "1.5 km"),
                        new Attraction("Parque Kennedy", "1.2 km"),
                        new Attraction("Malecón de Miraflores", "0.1 km")
                },
                "Suite Deluxe", "2 adultos, 1 niño", "45 m²",
                new String[]{"Cama King Size de lujo", "Imponente vista al mar", "Minibar inteligente", "Smart TV 55\" con domótica integrada"}
        ));
        add(new Hotel(
                2, "Cusco", "Palacio del Inka",
                "Centro Histórico, Cusco",
                4.9, 189,
                "Un palacio colonial restaurado en pleno centro histórico de Cusco, con " +
                        "habitaciones que fusionan tradición andina y tecnología conectada.",
                520.0,
                new String[]{"Wi-Fi ultra-veloz", "Desayuno incluido", "Oxigenación en habitación"},
                new Attraction[]{
                        new Attraction("Plaza de Armas", "0.3 km"),
                        new Attraction("Catedral del Cusco", "0.4 km"),
                        new Attraction("Qorikancha", "0.8 km")
                },
                "Suite Ejecutiva", "2 adultos", "38 m²",
                new String[]{"Cama Queen Size", "Vista a la plaza colonial", "Calefacción inteligente", "Smart TV 50\" con domótica integrada"}
        ));
        add(new Hotel(
                3, "Arequipa", "Casa Andina Premium",
                "Yanahuara, Arequipa",
                4.6, 142,
                "Con vista al Misti y a los volcanes que rodean la Ciudad Blanca, este hotel " +
                        "ofrece habitaciones amplias con control domótico integrado.",
                380.0,
                new String[]{"Wi-Fi ultra-veloz", "Desayuno incluido", "Estacionamiento gratuito"},
                new Attraction[]{
                        new Attraction("Mirador de Yanahuara", "0.2 km"),
                        new Attraction("Monasterio de Santa Catalina", "2 km"),
                        new Attraction("Plaza de Armas", "2.3 km")
                },
                "Habitación Superior", "2 adultos, 1 niño", "35 m²",
                new String[]{"Cama King Size", "Vista al volcán Misti", "Minibar", "Smart TV 43\" con domótica integrada"}
        ));
    }});

    public static Hotel findById(int id) {
        for (Hotel hotel : HOTELS) {
            if (hotel.id == id) {
                return hotel;
            }
        }
        return HOTELS.get(0);
    }

    public static final List<Reserva> RESERVAS = Collections.unmodifiableList(new ArrayList<Reserva>() {{
        add(new Reserva(101, 1, "Suite Deluxe", "15 – 18 ago", "2 adultos, 1 niño",
                3, 1350.0, Reserva.Estado.CONFIRMADA, "IOT-1042-7841"));
        add(new Reserva(102, 2, "Suite Ejecutiva", "2 – 4 oct", "2 adultos",
                2, 1040.0, Reserva.Estado.PENDIENTE_PAGO, "IOT-2078-5521"));
        add(new Reserva(103, 3, "Habitación Superior", "10 – 12 jun", "2 adultos, 1 niño",
                2, 760.0, Reserva.Estado.COMPLETADA, "IOT-3011-2290"));
        add(new Reserva(104, 1, "Suite Deluxe", "20 – 22 may", "2 adultos",
                2, 900.0, Reserva.Estado.CANCELADA, "IOT-1042-1187"));
    }});

    public static Reserva findReservaById(int id) {
        for (Reserva reserva : RESERVAS) {
            if (reserva.id == id) {
                return reserva;
            }
        }
        return RESERVAS.get(0);
    }

    public static final UserProfile PERFIL = new UserProfile(
            "Carlos Mendoza", "carlos.mendoza@gmail.com", "+51 987 654 321");

    /** Conversación estática de ejemplo con el hotel de una reserva. */
    public static List<Mensaje> chatDeReserva(String nombreHotel) {
        List<Mensaje> mensajes = new ArrayList<>();
        mensajes.add(new Mensaje("¡Hola! Bienvenido a " + nombreHotel + ". ¿En qué podemos ayudarte con tu reserva?",
                false, "09:14 a.m."));
        mensajes.add(new Mensaje("Hola, quería confirmar la hora de check-in.", true, "09:20 a.m."));
        mensajes.add(new Mensaje("El check-in es a partir de las 3:00 p.m. Si llegas antes, con gusto guardamos tu equipaje.",
                false, "09:22 a.m."));
        mensajes.add(new Mensaje("Perfecto, muchas gracias.", true, "09:23 a.m."));
        return mensajes;
    }
}
