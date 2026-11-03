package com.example.proyecto_iotelito.data.hoteladmin;

import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;
import com.example.proyecto_iotelito.model.hoteladmin.Servicio;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class HotelAdminFirestoreCodecTest {
    private Habitacion room(int id, boolean available, double price) {
        return new Habitacion(id, 17, "Doble", "Hab. " + id, 20, 2, 0, price, "Vista al mar", 0, available);
    }
    @Test public void requiresBothSidesOfAssignmentAndActiveHotel() {
        Map<String,Object> user = new HashMap<>(), hotel = new HashMap<>();
        user.put("uid", "admin"); user.put("role", "HOTEL_ADMIN"); user.put("active", true); user.put("hotelId", 17L);
        hotel.put("id", 17L); hotel.put("administratorUid", "admin"); hotel.put("active", true);
        HotelAdminFirestoreCodec.assigned("admin", 17, user, hotel);
        hotel.put("administratorUid", "another");
        assertThrows(IllegalArgumentException.class, () -> HotelAdminFirestoreCodec.assigned("admin", 17, user, hotel));
        hotel.put("administratorUid", "admin"); user.put("hotelId", 18L);
        assertThrows(IllegalArgumentException.class, () -> HotelAdminFirestoreCodec.assigned("admin", 17, user, hotel));
        user.put("hotelId", 17L); hotel.put("active", false);
        assertThrows(IllegalArgumentException.class, () -> HotelAdminFirestoreCodec.assigned("admin", 17, user, hotel));
    }
    @Test public void preservesPhysicalRoomIdentityAndCents() {
        Habitacion original = room(201, true, 135.75);
        Map<String,Object> data = HotelAdminFirestoreCodec.room(original);
        Habitacion decoded = HotelAdminFirestoreCodec.room("201", 17, data, 0);
        assertEquals(201, decoded.id); assertEquals(17, decoded.hotelId); assertEquals(135.75, decoded.precioNoche, 0.001);
        assertThrows(IllegalArgumentException.class, () -> HotelAdminFirestoreCodec.room("202", 17, data, 0));
        assertThrows(IllegalArgumentException.class, () -> HotelAdminFirestoreCodec.room("201", 18, data, 0));
    }
    @Test public void rejectsInvalidPricesAndCapacity() {
        assertThrows(IllegalArgumentException.class, () -> HotelAdminFirestoreCodec.room(room(1, true, Double.NaN)));
        assertThrows(IllegalArgumentException.class, () -> HotelAdminFirestoreCodec.room(room(1, true, -1)));
        Map<String,Object> data = HotelAdminFirestoreCodec.room(room(1, true, 100)); data.put("adultos", 0);
        assertThrows(IllegalArgumentException.class, () -> HotelAdminFirestoreCodec.room("1", 17, data, 0));
    }
    @Test public void summaryUsesAvailableRoomsAndHandlesEmptyInventory() {
        Map<String,Object> summary = HotelAdminFirestoreCodec.inventory(Arrays.asList(room(1, false, 1), room(2, true, 250), room(3, true, 180.50)));
        assertEquals(3, summary.get("rooms")); assertEquals(true, summary.get("hasInventory"));
        assertEquals(180.50, (Double) summary.get("pricePerNight"), 0.001);
        summary = HotelAdminFirestoreCodec.inventory(Collections.emptyList());
        assertEquals(0, summary.get("rooms")); assertEquals(false, summary.get("hasInventory"));
        assertEquals(0.0, (Double) summary.get("pricePerNight"), 0.0);
    }
    @Test public void serviceKeepsPriceUnitAndActiveStatus() {
        Servicio service = new Servicio(4, 17, "Desayuno", "Buffet", 42.50, "persona", 0, false);
        Servicio decoded = HotelAdminFirestoreCodec.service("4", 17, HotelAdminFirestoreCodec.service(service), 0);
        assertEquals("persona", decoded.unidad); assertFalse(decoded.activo); assertEquals(42.5, decoded.precio, 0.0);
    }
}
