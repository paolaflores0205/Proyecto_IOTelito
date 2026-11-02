package com.example.proyecto_iotelito.data.superadmin;

import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import java.util.*;

/** Contrato puro Java: IDs estables, ubicación y separación entre fotos compartidas y privadas. */
public final class HotelFirestoreCodec {
    public static final int FIRST_NEW_ID = 1_000_000;
    private HotelFirestoreCodec() { }
    public static int integer(Object value) {
        if (!(value instanceof Number)) throw new IllegalArgumentException("ID o contador no numérico.");
        double number = ((Number) value).doubleValue();
        if (!Double.isFinite(number) || number != Math.rint(number) || number < 0 || number > Integer.MAX_VALUE)
            throw new IllegalArgumentException("ID o contador fuera de rango.");
        return (int) number;
    }
    private static String text(Object value) { return value instanceof String ? ((String) value).trim() : ""; }
    private static int count(Map<String, Object> data, String key) {
        return data.get(key) == null ? 0 : integer(data.get(key));
    }
    private static double number(Object value) {
        if (value == null) return 0;
        if (!(value instanceof Number) || !Double.isFinite(((Number) value).doubleValue()) || ((Number) value).doubleValue() < 0)
            throw new IllegalArgumentException("Importe o valoración no válidos.");
        return ((Number) value).doubleValue();
    }
    public static List<String> sharedPhotos(List<String> photos) {
        List<String> result = new ArrayList<>();
        for (String photo : photos) {
            if (photo == null || photo.length() > 2048) continue;
            boolean shared = photo.startsWith("android.resource://com.example.proyecto_iotelito/drawable/");
            try {
                java.net.URI uri = java.net.URI.create(photo);
                shared |= "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null;
            } catch (IllegalArgumentException ignored) { }
            if (shared && !result.contains(photo) && result.size() < 8) result.add(photo);
        }
        return result;
    }
    public static ManagedHotel decode(String documentId, Map<String, Object> data, int fallbackImage) {
        if (data == null) throw new IllegalArgumentException("Hotel inexistente.");
        int id = integer(data.get("id"));
        if (id <= 0 || !String.valueOf(id).equals(documentId) || text(data.get("name")).isEmpty()
                || text(data.get("address")).isEmpty() || text(data.get("city")).isEmpty()
                || !(data.get("active") instanceof Boolean)) throw new IllegalArgumentException("Perfil de hotel incompleto.");
        String uid = text(data.get("administratorUid"));
        ManagedHotel result = new ManagedHotel(id, text(data.get("name")), text(data.get("address")),
                text(data.get("district")), uid.isEmpty() ? "Sin asignar" : text(data.get("administratorName")),
                count(data, "rooms"), count(data, "reservations"), fallbackImage, (Boolean) data.get("active"));
        result.remote = true; result.administratorUid = uid.isEmpty() ? null : uid;
        result.city = text(data.get("city")); result.phone = text(data.get("phone")); result.email = text(data.get("email"));
        result.description = text(data.get("description")); result.attractions = text(data.get("attractions"));
        Object lat = data.get("latitude"), lon = data.get("longitude");
        if (lat != null || lon != null) {
            if (!(lat instanceof Number) || !(lon instanceof Number)) throw new IllegalArgumentException("Ubicación incompleta.");
            result.latitude = ((Number) lat).doubleValue(); result.longitude = ((Number) lon).doubleValue();
            validateLocation(result.latitude, result.longitude);
        }
        result.pricePerNight = number(data.get("pricePerNight")); result.rating = number(data.get("rating"));
        result.reviews = count(data, "reviews"); result.hasInventory = Boolean.TRUE.equals(data.get("hasInventory"));
        if (data.get("services") instanceof List<?>) for (Object service : (List<?>) data.get("services"))
            if (service instanceof String) result.services.add((String) service);
        List<String> photos = new ArrayList<>();
        if (data.get("photos") instanceof List<?>) for (Object photo : (List<?>) data.get("photos"))
            if (photo instanceof String) photos.add((String) photo);
        result.photos = sharedPhotos(photos);
        return result;
    }
    public static void validateLocation(Double latitude, Double longitude) {
        if (latitude == null || longitude == null || !Double.isFinite(latitude) || !Double.isFinite(longitude)
                || Math.abs(latitude) > 90 || Math.abs(longitude) > 180)
            throw new IllegalArgumentException("Selecciona una ubicación válida en el mapa.");
    }
    public static Map<String, Object> editable(ManagedHotel hotel, boolean importing) {
        if (hotel.name.trim().isEmpty() || hotel.address.trim().isEmpty() || hotel.city.trim().isEmpty()
                || hotel.district.trim().isEmpty()) throw new IllegalArgumentException("Completa nombre, dirección, distrito y ciudad.");
        if (!importing || hotel.latitude != null || hotel.longitude != null) validateLocation(hotel.latitude, hotel.longitude);
        Map<String, Object> data = new HashMap<>();
        data.put("name", hotel.name.trim()); data.put("address", hotel.address.trim());
        data.put("district", hotel.district.trim()); data.put("city", hotel.city.trim());
        data.put("phone", hotel.phone.trim()); data.put("email", hotel.email.trim());
        data.put("description", hotel.description.trim()); data.put("active", hotel.active);
        data.put("latitude", hotel.latitude); data.put("longitude", hotel.longitude);
        data.put("photos", sharedPhotos(hotel.photos));
        return data;
    }
}
