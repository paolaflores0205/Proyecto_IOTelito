package com.example.proyecto_iotelito.data;

import org.junit.Test;
import static org.junit.Assert.*;

public class HotelPhotoPolicyTest {
    private static final String ID = "12345678-1234-1234-1234-123456789abc";
    private static final String CATALOG = "s3://iot-telito-imagenes/hotelPhotos/catalog/" + ID + ".jpg";
    private static final String HOTEL = "s3://iot-telito-imagenes/hotelPhotos/17/" + ID + ".jpg";

    @Test public void acceptsCatalogAndHotelFolders() {
        assertTrue(HotelPhotoPolicy.reference(CATALOG));
        assertTrue(HotelPhotoPolicy.reference(HOTEL));
        assertTrue(HotelPhotoPolicy.ofHotel(HOTEL, 17));
        assertFalse(HotelPhotoPolicy.ofHotel(HOTEL, 18));
        assertFalse(HotelPhotoPolicy.ofHotel(CATALOG, 17));
    }

    @Test public void rejectsOtherBucketsFoldersAndNames() {
        for (String bad : new String[]{
                null, "", HOTEL.replace("iot-telito-imagenes", "otro"),
                "s3://iot-telito-imagenes/profilePhotos/uid/" + ID + ".jpg",
                "s3://iot-telito-imagenes/hotelPhotos/0/" + ID + ".jpg",
                "s3://iot-telito-imagenes/hotelPhotos/../" + ID + ".jpg",
                "s3://iot-telito-imagenes/hotelPhotos/17/foto.jpg",
                HOTEL + "/extra", HOTEL.replace(".jpg", ".png"),
                "https://iot-telito-imagenes.s3.amazonaws.com/hotelPhotos/17/" + ID + ".jpg"}) {
            assertFalse(String.valueOf(bad), HotelPhotoPolicy.reference(bad));
        }
    }

    @Test public void legacyFirebaseReferencesRemainReadable() {
        assertTrue(HotelPhotoPolicy.remote("gs://iot-telito.firebasestorage.app/hotelPhotos/uid/a.jpg"));
        assertTrue(HotelPhotoPolicy.remote("gs://bucket/hotelAdminPhotos/17/admin/a.jpg"));
        assertFalse(HotelPhotoPolicy.remote("gs://bucket/users/private.jpg"));
        assertTrue(HotelPhotoPolicy.remote(CATALOG));
    }
}
