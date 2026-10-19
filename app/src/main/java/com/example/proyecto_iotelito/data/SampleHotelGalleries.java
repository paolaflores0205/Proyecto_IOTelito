package com.example.proyecto_iotelito.data;

import java.util.*;

/** Fotos de stock locales para previsualizar el carrusel; no son fotos oficiales del hotel. */
public final class SampleHotelGalleries {
    private static final String PREFIX = "android.resource://com.example.proyecto_iotelito/drawable/hotel_demo_";
    private static final String[][] SETS = {
        {"facade", "bedroom", "pool", "suite"},
        {"resort", "suite", "room", "terrace"},
        {"pool", "bedroom", "facade", "lobby"},
        {"lobby", "room", "terrace", "suite"}
    };
    private SampleHotelGalleries() {}
    public static List<String> forHotel(int hotelId) {
        if (hotelId < 1 || hotelId > 12) return new ArrayList<>();
        String[] names = SETS[(hotelId - 1) % SETS.length];
        List<String> photos = new ArrayList<>();
        for (String name : names) photos.add(PREFIX + name);
        return photos;
    }
    public static boolean isIllustrative(String uri) { return uri != null && uri.startsWith(PREFIX); }
}
