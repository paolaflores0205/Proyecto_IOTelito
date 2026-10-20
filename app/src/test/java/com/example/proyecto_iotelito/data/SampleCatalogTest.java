package com.example.proyecto_iotelito.data;

import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.model.superadmin.AdminUser;
import com.example.proyecto_iotelito.model.superadmin.ManagedHotel;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

/** Comprueba la coherencia de los datos locales entre las pantallas de los roles. */
public class SampleCatalogTest {
    @Test
    public void hotelesCoincidenEnAmbosRoles() {
        assertEquals(12, SampleData.HOTELS.size());
        assertEquals(SampleData.HOTELS.size(), SuperadminSampleData.hotels().size());
        Set<Integer> ids = new HashSet<>();
        for (Hotel hotel : SampleData.HOTELS) {
            assertTrue("ID de hotel duplicado", ids.add(hotel.id));
            ManagedHotel gestion = SuperadminSampleData.hotel(hotel.id);
            assertEquals(hotel.id, gestion.id);
            assertEquals(hotel.name, gestion.name);
            assertEquals(hotel.address, gestion.address);
            assertEquals(HotelMedia.hotelImage(hotel.id), gestion.imageRes);
            assertTrue(hotel.pricePerNight > 0);
            assertTrue(hotel.roomFeatures.length > 0);
        }
    }

    @Test
    public void reservasExistentesConservanSuHotel() {
        assertEquals("Hotel Miraflores Park", SampleData.findById(1).name);
        assertEquals("Palacio del Inka", SampleData.findById(2).name);
        assertEquals("Casa Andina Premium", SampleData.findById(3).name);
        for (Reserva reserva : SampleData.RESERVAS) {
            assertEquals(reserva.hotelId, SampleData.findById(reserva.hotelId).id);
        }
    }

    @Test
    public void usuariosCubrenFiltrosYAdministradoresAsignados() {
        assertEquals(16, SuperadminSampleData.users().size());
        Set<Integer> ids = new HashSet<>();
        Set<String> roles = new HashSet<>();
        Set<String> administradores = new HashSet<>();
        boolean hayActivos = false;
        boolean hayInactivos = false;
        for (AdminUser user : SuperadminSampleData.users()) {
            assertTrue("ID de usuario duplicado", ids.add(user.id));
            roles.add(user.role);
            hayActivos |= user.active;
            hayInactivos |= !user.active;
            if (user.active && "Administrador".equals(user.role)) administradores.add(user.name);
        }
        assertEquals(4, roles.size());
        assertTrue(hayActivos && hayInactivos);
        for (ManagedHotel hotel : SuperadminSampleData.hotels()) {
            assertTrue("Administrador inexistente: " + hotel.administrator,
                    "Sin asignar".equals(hotel.administrator) || administradores.contains(hotel.administrator));
        }
    }
}
