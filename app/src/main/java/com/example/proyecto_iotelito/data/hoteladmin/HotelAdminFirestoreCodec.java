package com.example.proyecto_iotelito.data.hoteladmin;

import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;
import com.example.proyecto_iotelito.model.hoteladmin.Servicio;
import com.example.proyecto_iotelito.data.superadmin.HotelFirestoreCodec;
import java.util.*;

/** Formato compartido de inventario y servicios adicionales. No depende de Android. */
public final class HotelAdminFirestoreCodec {
    private HotelAdminFirestoreCodec() { }
    private static String text(Object value, int max, boolean required) {
        if (!(value instanceof String)) throw new IllegalArgumentException("Texto incompleto.");
        String text = ((String) value).trim();
        if ((required && text.isEmpty()) || text.length() > max)
            throw new IllegalArgumentException("Revisa los campos obligatorios y su longitud.");
        return text;
    }
    private static double money(Object value) {
        if (!(value instanceof Number)) throw new IllegalArgumentException("Precio no válido.");
        double price = ((Number) value).doubleValue();
        if (!Double.isFinite(price) || price < 0 || price > 1000000)
            throw new IllegalArgumentException("El precio debe estar entre 0 y 1 000 000.");
        return price;
    }
    public static void assigned(String uid, int hotelId, Map<String,Object> profile, Map<String,Object> hotel) {
        if (profile == null || hotel == null || !uid.equals(profile.get("uid"))
                || !"HOTEL_ADMIN".equals(profile.get("role")) || !Boolean.TRUE.equals(profile.get("active"))
                || HotelFirestoreCodec.integer(profile.get("hotelId")) != hotelId
                || HotelFirestoreCodec.integer(hotel.get("id")) != hotelId
                || !uid.equals(hotel.get("administratorUid")) || !Boolean.TRUE.equals(hotel.get("active")))
            throw new IllegalArgumentException("No tienes un hotel activo asignado. Consulta al superadmin.");
    }
    private static int id(String documentId, int hotelId, Map<String,Object> data) {
        int id = HotelFirestoreCodec.integer(data.get("id"));
        if (id <= 0 || !String.valueOf(id).equals(documentId)
                || HotelFirestoreCodec.integer(data.get("hotelId")) != hotelId)
            throw new IllegalArgumentException("El registro no pertenece a este hotel.");
        return id;
    }
    public static Map<String,Object> room(Habitacion room) {
        Map<String,Object> data = new HashMap<>();
        data.put("id", room.id); data.put("hotelId", room.hotelId); data.put("tipo", room.tipo);
        data.put("nombreCodigo", room.nombreCodigo); data.put("areaM2", room.areaM2);
        data.put("adultos", room.adultos); data.put("ninos", room.ninos); data.put("precioNoche", room.precioNoche);
        data.put("descripcion", room.descripcion); data.put("disponible", room.disponible); data.put("archived", false);
        room(String.valueOf(room.id), room.hotelId, data, room.fotoRes);
        return data;
    }
    public static Habitacion room(String key, int hotelId, Map<String,Object> data, int image) {
        int id = id(key, hotelId, data);
        int area = HotelFirestoreCodec.integer(data.get("areaM2"));
        int adults = HotelFirestoreCodec.integer(data.get("adultos")), kids = HotelFirestoreCodec.integer(data.get("ninos"));
        if (area < 1 || area > 10000 || adults < 1 || adults > 100 || kids > 100
                || !(data.get("disponible") instanceof Boolean))
            throw new IllegalArgumentException("Revisa área, capacidad y disponibilidad.");
        return new Habitacion(id, hotelId, text(data.get("tipo"), 120, true),
                text(data.get("nombreCodigo"), 80, true), area, adults, kids, money(data.get("precioNoche")),
                text(data.get("descripcion"), 5000, false), image, (Boolean) data.get("disponible"));
    }
    public static Map<String,Object> service(Servicio service) {
        Map<String,Object> data = new HashMap<>();
        data.put("id", service.id); data.put("hotelId", service.hotelId); data.put("nombre", service.nombre);
        data.put("descripcion", service.descripcion); data.put("precio", service.precio);
        data.put("unidad", service.unidad); data.put("activo", service.activo); data.put("archived", false);
        service(String.valueOf(service.id), service.hotelId, data, service.fotoRes);
        return data;
    }
    public static Servicio service(String key, int hotelId, Map<String,Object> data, int image) {
        int id = id(key, hotelId, data);
        if (!(data.get("activo") instanceof Boolean)) throw new IllegalArgumentException("Estado no válido.");
        return new Servicio(id, hotelId, text(data.get("nombre"), 160, true),
                text(data.get("descripcion"), 5000, false), money(data.get("precio")),
                text(data.get("unidad"), 80, true), image, (Boolean) data.get("activo"));
    }
    public static Map<String,Object> inventory(List<Habitacion> rooms) {
        Map<String,Object> result = new HashMap<>();
        double min = Double.POSITIVE_INFINITY;
        for (Habitacion room : rooms) if (room.disponible) min = Math.min(min, room.precioNoche);
        result.put("rooms", rooms.size()); result.put("hasInventory", Double.isFinite(min));
        result.put("pricePerNight", Double.isFinite(min) ? min : 0);
        return result;
    }
}
