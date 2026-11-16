package com.example.proyecto_iotelito.data;

import java.net.URI;
import java.util.regex.Pattern;

/** Referencias persistentes y destinos de descarga permitidos para las fotos privadas. */
public final class ProfilePhotoPolicy {
    public static final String API = "https://9s5rtfhae6.execute-api.us-east-1.amazonaws.com";
    public static final String BUCKET = "iot-telito-imagenes";
    public static final int MAX_BYTES = 2 * 1024 * 1024 - 1;
    private static final String UUID = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";
    private ProfilePhotoPolicy() { }
    public static boolean propia(String reference, String uid) {
        return reference != null && uid != null && !uid.isEmpty()
                && reference.matches("s3://" + BUCKET + "/profilePhotos/" + Pattern.quote(uid) + "/" + UUID + "[.]jpg");
    }
    public static boolean destinoS3(String value) {
        try {
            URI uri = new URI(value);
            return "https".equals(uri.getScheme()) && uri.getUserInfo() == null
                    && uri.getPort() == -1 && uri.getFragment() == null
                    && (String.valueOf(uri.getHost()).equals(BUCKET + ".s3.us-east-1.amazonaws.com")
                    || String.valueOf(uri.getHost()).equals(BUCKET + ".s3.amazonaws.com"));
        } catch (Exception error) { return false; }
    }
}
