package com.example.proyecto_iotelito.data.superadmin;

import android.content.Context;
import com.example.proyecto_iotelito.data.HotelAwsPhotos;
import com.google.android.gms.tasks.Task;
import java.util.List;

/**
 * Sube las copias JPEG privadas al bucket de AWS; Firestore conserva referencias s3:// estables.
 * La API no borra objetos: una carga que no llega a guardarse deja un archivo privado sin referencia.
 */
public final class HotelCloudPhotos {
    private HotelCloudPhotos() { }

    /** Superadmin: las fotos nuevas van a la carpeta de catálogo. */
    public static Task<List<String>> upload(Context context, List<String> photos) {
        return HotelAwsPhotos.upload(context, null, photos);
    }

    /** Administrador asignado: las fotos nuevas van a la carpeta de su hotel. */
    public static Task<List<String>> uploadForHotel(Context context, int hotelId, List<String> photos) {
        return HotelAwsPhotos.upload(context, hotelId, photos);
    }
}
