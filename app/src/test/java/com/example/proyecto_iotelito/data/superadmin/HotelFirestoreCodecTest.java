package com.example.proyecto_iotelito.data.superadmin;

import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class HotelFirestoreCodecTest {
    private ManagedHotel hotel() {
        ManagedHotel h = new ManagedHotel(2, "Hotel", "Av. 100", "Miraflores", "Sin asignar", 0, 0, 1, true);
        h.city = "Lima"; h.latitude = -12.1; h.longitude = -77.1;
        return h;
    }
    private Map<String, Object> document() {
        Map<String, Object> map = HotelFirestoreCodec.editable(hotel(), false);
        map.put("id", 2L); return map;
    }
    @Test public void keepsIdAndRemoteMetadataWithoutLocalAssignment() {
        ManagedHotel remote = HotelFirestoreCodec.decode("2", document(), 1);
        assertEquals(2, remote.id); assertTrue(remote.remote); assertFalse(remote.hasAdministrator());
    }
    @Test public void detectsAssignmentByUidNotLegacyInteger() {
        Map<String, Object> map = document(); map.put("administratorUid", "real-uid"); map.put("administratorName", "Ana");
        ManagedHotel remote = HotelFirestoreCodec.decode("2", map, 1);
        assertTrue(remote.hasAdministrator()); assertNull(remote.administratorId);
        assertEquals("real-uid", remote.administratorUid);
    }
    @Test public void rejectsIdMismatchAndOverflow() {
        assertThrows(IllegalArgumentException.class, () -> HotelFirestoreCodec.decode("3", document(), 1));
        assertThrows(IllegalArgumentException.class, () -> HotelFirestoreCodec.integer(2147483648L));
        assertThrows(IllegalArgumentException.class, () -> HotelFirestoreCodec.integer(1.5));
    }
    @Test public void normalSaveRequiresRealCoordinates() {
        ManagedHotel value = hotel(); value.latitude = null; value.longitude = null;
        assertThrows(IllegalArgumentException.class, () -> HotelFirestoreCodec.editable(value, false));
        assertNull(HotelFirestoreCodec.editable(value, true).get("latitude"));
    }
    @Test public void rejectsNonFiniteAndOutOfRangeCoordinates() {
        assertThrows(IllegalArgumentException.class, () -> HotelFirestoreCodec.validateLocation(Double.NaN, 0.0));
        assertThrows(IllegalArgumentException.class, () -> HotelFirestoreCodec.validateLocation(91.0, 0.0));
    }
    @Test public void privatePhotosNeverBecomeRemoteReferences() {
        List<String> refs = HotelFirestoreCodec.sharedPhotos(Arrays.asList("file:///private/a.jpg", "content://gallery/1",
                "https://example.test/photo.jpg", "android.resource://com.example.proyecto_iotelito/drawable/hotel_demo_1", "javascript:bad"));
        assertEquals(2, refs.size()); assertFalse(refs.get(0).startsWith("file:"));
    }
    @Test public void editsNeverContainAssignmentInventoryOrStatistics() {
        Map<String, Object> map = HotelFirestoreCodec.editable(hotel(), false);
        for (String field : Arrays.asList("id", "administratorUid", "administratorName", "rooms", "reservations", "pricePerNight"))
            assertFalse(map.containsKey(field));
    }
    @Test public void storageReferencesSurviveWithoutPublishingLocalUris() {
        String photo = "gs://iot-telito.firebasestorage.app/hotelPhotos/uid/photo.jpg";
        assertEquals(Collections.singletonList(photo), HotelFirestoreCodec.sharedPhotos(Arrays.asList(photo, "file:///private.jpg")));
    }
    @Test public void arbitraryStoragePathsAreRejected() {
        assertTrue(HotelFirestoreCodec.sharedPhotos(Arrays.asList("gs://bucket/users/private.jpg", "gs://bucket/hotelPhotos/u/file.exe")).isEmpty());
    }
}
