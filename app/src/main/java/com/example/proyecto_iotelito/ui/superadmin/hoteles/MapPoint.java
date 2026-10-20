package com.example.proyecto_iotelito.ui.superadmin.hoteles;

/** Tolera el redondeo subpíxel del mapa sin considerar que se eligió otra ubicación. */
final class MapPoint {
    private MapPoint() {}

    static boolean same(double latitudeA, double longitudeA, double latitudeB, double longitudeB) {
        if (!Double.isFinite(latitudeA) || !Double.isFinite(longitudeA)
                || !Double.isFinite(latitudeB) || !Double.isFinite(longitudeB)) return false;
        double latitudeDelta = Math.toRadians(latitudeB - latitudeA);
        double longitudeDelta = Math.toRadians(longitudeB - longitudeA);
        double a = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(Math.toRadians(latitudeA)) * Math.cos(Math.toRadians(latitudeB))
                * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double distance = 6371000 * 2 * Math.asin(Math.sqrt(Math.min(1, a)));
        return distance < 2;
    }
}
