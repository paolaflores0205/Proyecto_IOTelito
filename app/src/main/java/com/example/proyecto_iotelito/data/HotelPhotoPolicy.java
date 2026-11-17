package com.example.proyecto_iotelito.data;

/** Referencias S3 estables de fotos de hotel; Firestore nunca guarda enlaces firmados. */
public final class HotelPhotoPolicy {
    public static final String CATALOG = "catalog";
    private static final String PREFIX = "s3://" + ProfilePhotoPolicy.BUCKET + "/hotelPhotos/";
    private static final String UUID_JPG = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}[.]jpg";
    private HotelPhotoPolicy() { }

    /** Foto de hotel en S3: catálogo del superadmin o carpeta de un hotel. */
    public static boolean reference(String value) {
        return value != null && value.matches(java.util.regex.Pattern.quote(PREFIX)
                + "(" + CATALOG + "|[1-9][0-9]{0,9})/" + UUID_JPG);
    }

    /** Foto subida por el administrador asignado a ese hotel. */
    public static boolean ofHotel(String value, int hotelId) {
        return hotelId > 0 && value != null
                && value.matches(java.util.regex.Pattern.quote(PREFIX + hotelId + "/") + UUID_JPG);
    }

    /** Referencias anteriores en Firebase Storage que se siguen aceptando al leer datos existentes. */
    public static boolean legacyHotel(String value) {
        return value != null && (value.matches("gs://[^/]+/hotelPhotos/[^/]+/[^/]+[.]jpg")
                || value.matches("gs://[^/]+/hotelAdminPhotos/[0-9]+/[^/]+/[^/]+[.]jpg"));
    }

    public static boolean remote(String value) {
        return reference(value) || legacyHotel(value);
    }
}
