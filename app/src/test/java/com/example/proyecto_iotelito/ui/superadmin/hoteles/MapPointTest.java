package com.example.proyecto_iotelito.ui.superadmin.hoteles;

import org.junit.Test;
import static org.junit.Assert.*;

public class MapPointTest {
    @Test public void identicalCenterIsNotANewSelection() {
        assertTrue(MapPoint.same(-12.0464, -77.0428, -12.0464, -77.0428));
    }

    @Test public void subpixelRoundingDoesNotClearSelection() {
        assertTrue(MapPoint.same(-12.0464, -77.0428, -12.046390, -77.0428));
    }

    @Test public void realMovementChangesSelection() {
        assertFalse(MapPoint.same(-12.0464, -77.0428, -12.046350, -77.0428));
    }

    @Test public void comparesLongitudeAsWellAsLatitude() {
        assertFalse(MapPoint.same(-12.0464, -77.0428, -12.0464, -77.0427));
    }

    @Test public void invalidCoordinatesAreNotEqual() {
        assertFalse(MapPoint.same(Double.NaN, -77, -12, -77));
    }
}
