package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.R;

/** Recursos locales usados en el prototipo mientras no existe almacenamiento remoto. */
public final class HotelMedia {
    private static final int[] HOTEL_IMAGES = {
            R.drawable.superadmin_hotel_sol,
            R.drawable.superadmin_hotel_hero,
            R.drawable.superadmin_hotel_barranco
    };

    private HotelMedia() { }

    public static int hotelImage(int hotelId) {
        int position = Math.max(0, hotelId - 1) % HOTEL_IMAGES.length;
        return HOTEL_IMAGES[position];
    }

    public static int galleryImage(int hotelId, int position) {
        return HOTEL_IMAGES[(Math.max(0, hotelId - 1) + position) % HOTEL_IMAGES.length];
    }
}
