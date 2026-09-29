package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.Attraction;
import com.example.proyecto_iotelito.model.Conversacion;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Mensaje;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.UserProfile;

import java.time.LocalDate;
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
        add(hotelDemo(4, "Lima", "Hotel Sol de Miraflores", "Av. Larco 345, Miraflores, Lima",
                4.5, 128, 320, "Suite con balcón", "32 m²", "Parque Kennedy", "0.5 km"));
        add(hotelDemo(5, "Lima", "Barranco Art Boutique", "Jr. Centenario 102, Barranco, Lima",
                4.8, 96, 290, "Habitación Boutique", "28 m²", "Puente de los Suspiros", "0.4 km"));
        add(hotelDemo(6, "Lima", "San Isidro Grand Suite", "Av. Camino Real 820, San Isidro, Lima",
                4.7, 175, 410, "Suite Ejecutiva", "42 m²", "Bosque El Olivar", "0.3 km"));
        add(hotelDemo(7, "Lima", "Barranco Garden House", "Av. San Martín 420, Barranco, Lima",
                4.3, 87, 240, "Habitación Jardín", "26 m²", "Museo de Arte Contemporáneo", "0.6 km"));
        add(hotelDemo(8, "Lima", "Surco Business Hotel", "Av. Primavera 1050, Surco, Lima",
                4.4, 112, 280, "Habitación Ejecutiva", "30 m²", "Parque de la Amistad", "2 km"));
        add(hotelDemo(9, "Cusco", "Cusco Plaza Inn", "Calle Plateros 250, Centro Histórico, Cusco",
                4.6, 154, 310, "Habitación Colonial", "29 m²", "Plaza de Armas", "0.2 km"));
        add(hotelDemo(10, "Arequipa", "Misti Terra Hotel", "Calle Jerusalén 406, Cercado, Arequipa",
                4.5, 103, 260, "Suite Misti", "34 m²", "Monasterio de Santa Catalina", "0.5 km"));
        add(hotelDemo(11, "Trujillo", "Trujillo Centro Suites", "Jr. Independencia 485, Centro Histórico, Trujillo",
                4.2, 78, 230, "Suite Familiar", "36 m²", "Plaza de Armas", "0.3 km"));
        add(hotelDemo(12, "Paracas", "Paracas Costa Azul", "Av. Paracas 180, Paracas, Ica",
                4.8, 201, 460, "Suite Vista al Mar", "44 m²", "Malecón de Paracas", "0.1 km"));
    }});

    private static Hotel hotelDemo(int id, String ciudad, String nombre, String direccion,
                                   double valoracion, int opiniones, double precio,
                                   String habitacion, String area, String atractivo, String distancia) {
        return new Hotel(id, ciudad, nombre, direccion, valoracion, opiniones,
                "Hospedaje de demostración en " + ciudad + ". Disfruta de habitaciones cómodas, "
                        + "atención personalizada y servicios conectados durante tu estadía.",
                precio, new String[]{"Wi-Fi ultra-veloz", "Desayuno incluido", "Estacionamiento gratuito"},
                new Attraction[]{new Attraction(atractivo, distancia)},
                habitacion, "2 adultos, 1 niño", area,
                new String[]{"Cama Queen Size", "Aire acondicionado", "Minibar", "Smart TV con domótica integrada"});
    }

    // El mismo catálogo se usa para construir los hoteles del superadmin.
    // Se conservan los IDs 1, 2 y 3 porque las reservas existentes los referencian.
    public static Hotel findById(int id) {
        for (Hotel hotel : HOTELS) {
            if (hotel.id == id) {
                return hotel;
            }
        }
        return HOTELS.get(0);
    }

    public static final List<Reserva> RESERVAS;
    static {
        LocalDate hoy = LocalDate.now();
        List<Reserva> lista = new ArrayList<>();
        // Próxima estadía: empieza en 6 días.
        lista.add(new Reserva(101, 1, "Suite Deluxe", "310",
                hoy.plusDays(6), hoy.plusDays(9), "2 adultos, 1 niño",
                1350.0, Reserva.Estado.CONFIRMADA, "IOT-1042-7841"));
        // En curso y HOY es el día de salida: ya se puede hacer el checkout.
        lista.add(new Reserva(102, 2, "Suite Ejecutiva", "804",
                hoy.minusDays(3), hoy, "2 adultos",
                1560.0, Reserva.Estado.CONFIRMADA, "IOT-2078-5521"));
        // Finalizadas
        lista.add(new Reserva(103, 3, "Habitación Superior", "212",
                hoy.minusDays(96), hoy.minusDays(94), "2 adultos, 1 niño",
                760.0, Reserva.Estado.COMPLETADA, "IOT-3011-2290"));
        lista.add(new Reserva(104, 1, "Suite Deluxe", "310",
                hoy.minusDays(117), hoy.minusDays(115), "2 adultos",
                900.0, Reserva.Estado.CANCELADA, "IOT-1042-1187"));
        RESERVAS = lista;
    }

    /** Añade una reserva confirmada durante la sesión local actual. */
    public static synchronized Reserva agregarReserva(
            Hotel hotel,
            LocalDate fechaEntrada,
            LocalDate fechaSalida,
            String huespedes,
            double precioTotal,
            String codigo) {
        int nuevoId = 1;
        for (Reserva reserva : RESERVAS) {
            nuevoId = Math.max(nuevoId, reserva.id + 1);
        }
        Reserva nuevaReserva = new Reserva(
                nuevoId,
                hotel.id,
                hotel.roomName,
                "Por asignar",
                fechaEntrada,
                fechaSalida,
                huespedes,
                precioTotal,
                Reserva.Estado.CONFIRMADA,
                codigo);
        RESERVAS.add(nuevaReserva);
        return nuevaReserva;
    }

    public static Reserva findReservaById(int id) {
        for (Reserva reserva : RESERVAS) {
            if (reserva.id == id) {
                return reserva;
            }
        }
        return RESERVAS.get(0);
    }

    public static final UserProfile PERFIL = new UserProfile(
            "Carlos Benítez", "carlos.benitez@email.com", "+51 987 654 321",
            "DNI", "45678912", "15/03/1990", "Av. Larco 456, Miraflores, Lima");

    /** Conversaciones estáticas de ejemplo para la pestaña de Mensajes (vista del cliente con sus hoteles). */
    public static final List<Conversacion> CONVERSACIONES = Collections.unmodifiableList(new ArrayList<Conversacion>() {{
        add(new Conversacion(1, 2, "Palacio del Inka", "Hab. 804",
                "¿Podrían traerme agua de cortesía a la habitación?", "09:15", 1, Conversacion.Categoria.HOSPEDADOS));
        add(new Conversacion(2, 1, "Hotel Miraflores Park", "Hab. 310",
                "Perfecto, muchas gracias por confirmar la hora de check-in.", "10:30", 0, Conversacion.Categoria.POR_LLEGAR));
        add(new Conversacion(4, 2, "Palacio del Inka", "Servicio Spa · Hab. 804",
                "Su reserva de spa para las 5:00 PM ha sido confirmada.", "Ayer", 2, Conversacion.Categoria.HOSPEDADOS));
        add(new Conversacion(5, 1, "Hotel Miraflores Park", "Hab. 104",
                "Gracias por alojarse con nosotros. ¡Esperamos volver a verle pronto!", "10 Jul", 0, Conversacion.Categoria.PASADOS));
    }});

    public static Conversacion findConversacionById(int id) {
        for (Conversacion conv : CONVERSACIONES) {
            if (conv.id == id) {
                return conv;
            }
        }
        return CONVERSACIONES.get(0);
    }

    /** Carga el historial completo de chat para una conversación específica. */
    public static List<Mensaje> chatDeConversacion(int conversacionId, String nombreHotel) {
        List<Mensaje> mensajes = new ArrayList<>();
        if (conversacionId == 1) {
            mensajes.add(new Mensaje("¡Hola Carlos! Bienvenido a " + nombreHotel + ". ¿En qué podemos ayudarte durante tu estadía en la Hab. 804?",
                    false, "09:00 a.m."));
            mensajes.add(new Mensaje("Hola, quería solicitar si me podrían traer agua de cortesía a la habitación.",
                    true, "09:10 a.m."));
            mensajes.add(new Mensaje("Por supuesto, en breve nuestro equipo de recepción enviará agua de cortesía a la Hab. 804.",
                    false, "09:12 a.m."));
            mensajes.add(new Mensaje("¿Podrían traerme agua de cortesía a la habitación?",
                    true, "09:15 a.m."));
            mensajes.add(new Mensaje("Servicio a la habitación va en camino. ¡Que disfrute su estadía!",
                    false, "09:16 a.m."));
        } else if (conversacionId == 2) {
            mensajes.add(new Mensaje("¡Hola! Gracias por elegir " + nombreHotel + ". Tu próxima estadía en la Suite Deluxe (Hab. 310) está confirmada.",
                    false, "10:15 a.m."));
            mensajes.add(new Mensaje("Hola, quería confirmar la hora exacta de check-in.",
                    true, "10:20 a.m."));
            mensajes.add(new Mensaje("El check-in oficial es a las 3:00 p.m. Si llegas más temprano, con gusto custodiamos tu equipaje.",
                    false, "10:25 a.m."));
            mensajes.add(new Mensaje("Perfecto, muchas gracias por confirmar la hora de check-in.",
                    true, "10:30 a.m."));
        } else if (conversacionId == 4) {
            mensajes.add(new Mensaje("Buenas tardes, ¿tienen disponibilidad en el Spa para el día de hoy?",
                    true, "Ayer 04:00 p.m."));
            mensajes.add(new Mensaje("¡Buenas tardes! Sí tenemos disponibilidad para circuito termal y masajes relajantes.",
                    false, "Ayer 04:10 p.m."));
            mensajes.add(new Mensaje("Me gustaría agendar una sesión a las 5:00 p.m. por favor.",
                    true, "Ayer 04:15 p.m."));
            mensajes.add(new Mensaje("Su reserva de spa para las 5:00 PM ha sido confirmada.",
                    false, "Ayer 04:22 p.m."));
        } else if (conversacionId == 5) {
            mensajes.add(new Mensaje("Esperamos que hayas disfrutado tu estadía en la Hab. 104 de " + nombreHotel + ".",
                    false, "10 Jul 11:00 a.m."));
            mensajes.add(new Mensaje("Muchas gracias, la atención y el servicio estuvieron excelentes.",
                    true, "10 Jul 11:30 a.m."));
            mensajes.add(new Mensaje("Gracias por alojarse con nosotros. ¡Esperamos volver a verle pronto!",
                    false, "10 Jul 11:45 a.m."));
        } else {
            return chatDeReserva(nombreHotel);
        }
        return mensajes;
    }

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
