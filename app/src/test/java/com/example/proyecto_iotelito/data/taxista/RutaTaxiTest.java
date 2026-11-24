package com.example.proyecto_iotelito.data.taxista;

import static org.junit.Assert.*;

import org.junit.Test;

public class RutaTaxiTest {
    @Test public void aeropuertoPorCodigoONombre() {
        assertArrayEquals(new double[] {-12.0219, -77.1143}, RutaTaxi.aeropuerto("LIM", null), 1e-9);
        assertArrayEquals(new double[] {-13.5357, -71.9388},
                RutaTaxi.aeropuerto("", "Aeropuerto Internacional Alejandro Velasco Astete"), 1e-9);
        assertArrayEquals(new double[] {-12.0219, -77.1143},
                RutaTaxi.aeropuerto(null, "Aeropuerto Internacional Jorge Chávez"), 1e-9);
        assertNotNull(RutaTaxi.aeropuerto("pio", ""));
        assertNull(RutaTaxi.aeropuerto("XXX", "Otro lugar"));
    }

    @Test public void urlPoneLongitudPrimero() {
        assertEquals("https://router.project-osrm.org/route/v1/driving/-77.036000,-12.096200;-77.114300,-12.021900"
                        + "?overview=full&geometries=geojson",
                RutaTaxi.url(-12.0962, -77.036, -12.0219, -77.1143));
    }

    @Test public void leeRespuestaDeOsrm() {
        String json = "{\"code\":\"Ok\",\"routes\":[{\"distance\":3210.4,\"duration\":540.2,"
                + "\"geometry\":{\"type\":\"LineString\",\"coordinates\":[[-77.03,-12.09],[-77.05,-12.08],[-77.11,-12.02]]}}]}";
        RutaTaxi.Ruta ruta = RutaTaxi.leer(json);
        assertNotNull(ruta);
        assertTrue(ruta.porCalles);
        assertEquals(3, ruta.puntos.size());
        assertArrayEquals(new double[] {-12.09, -77.03}, ruta.puntos.get(0), 1e-9);
        assertEquals("3.2 km · 9 min", ruta.resumen());
        assertNull(RutaTaxi.leer("{\"code\":\"NoRoute\",\"routes\":[]}"));
        assertNull(RutaTaxi.leer("no es json"));
    }

    @Test public void lineaRectaComoRespaldo() {
        RutaTaxi.Ruta ruta = RutaTaxi.recta(-12.0962, -77.036, -12.0219, -77.1143);
        assertFalse(ruta.porCalles);
        assertEquals(2, ruta.puntos.size());
        assertEquals(11870, ruta.metros, 100);
        assertTrue(ruta.resumen(), ruta.resumen().startsWith("≈ 11.9 km · 28 min"));
        assertTrue(ruta.resumen().endsWith("(en línea recta)"));
        assertEquals("450 m · 1 min", new RutaTaxi.Ruta(ruta.puntos, 450, 40, true).resumen());
    }

    @Test public void recalculaSoloCuandoHaceFalta() {
        double[] antes = {-12.0962, -77.036};
        assertTrue(RutaTaxi.recalcular(null, 0, true, -12.0962, -77.036, 1000));
        assertTrue(RutaTaxi.recalcular(antes, 0, false, -12.0962, -77.036, 1000));
        assertFalse(RutaTaxi.recalcular(antes, 0, true, -12.0963, -77.036, 5000));   // 11 m, 5 s
        assertTrue(RutaTaxi.recalcular(antes, 0, true, -12.0972, -77.036, 5000));    // 111 m
        assertFalse(RutaTaxi.recalcular(antes, 0, true, -12.0963, -77.036, 40000));  // parado 40 s
        assertTrue(RutaTaxi.recalcular(antes, 0, true, -12.0965, -77.036, 40000));   // 33 m y 40 s
    }
}
