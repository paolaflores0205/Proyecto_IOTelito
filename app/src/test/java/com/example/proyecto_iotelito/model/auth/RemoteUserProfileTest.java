package com.example.proyecto_iotelito.model.auth;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class RemoteUserProfileTest {
    private Map<String, Object> profile() {
        Map<String, Object> data = new HashMap<>();
        data.put("uid", "uid-1"); data.put("name", "Ana García"); data.put("email", "ana@example.test");
        data.put("role", "SUPERADMIN"); data.put("active", true);
        return data;
    }
    @Test public void resolvesServerSuperadminWithoutEmailHeuristics() {
        AuthenticatedUser user = RemoteUserProfile.parse("uid-1", profile()).authenticated();
        assertEquals(UserRole.SUPERADMIN, user.getRole()); assertEquals("uid-1", user.getUid());
    }
    @Test public void supportsAllFourCanonicalRoles() {
        for (UserRole role : UserRole.values()) {
            Map<String, Object> data = profile(); data.put("role", role.name());
            if (role == UserRole.HOTEL_ADMIN) data.put("hotelId", 1L);
            assertEquals(role, RemoteUserProfile.parse("uid-1", data).authenticated().getRole());
        }
    }
    @Test public void unassignedHotelAdminCannotEnterSampleHotel() {
        Map<String, Object> data = profile(); data.put("role", "HOTEL_ADMIN");
        assertThrows(IllegalArgumentException.class,
                () -> RemoteUserProfile.parse("uid-1", data).authenticated());
    }
    @Test public void carriesAssignedHotelFromFirestoreAndRejectsInvalidIds() {
        Map<String, Object> data = profile(); data.put("role", "HOTEL_ADMIN");
        data.put("hotelId", 17L);
        assertEquals(Integer.valueOf(17), RemoteUserProfile.parse("uid-1", data)
                .authenticated().getHotelId());
        data.put("hotelId", "17");
        assertThrows(IllegalArgumentException.class, () -> RemoteUserProfile.parse("uid-1", data));
        data.put("hotelId", 0L);
        assertThrows(IllegalArgumentException.class, () -> RemoteUserProfile.parse("uid-1", data));
    }
    @Test public void inactiveProfileCanBeListedButCannotAuthenticate() {
        Map<String, Object> data = profile(); data.put("active", false);
        RemoteUserProfile p = RemoteUserProfile.parse("uid-1", data);
        assertFalse(p.active); assertThrows(IllegalArgumentException.class, p::authenticated);
    }
    @Test public void rejectsMissingProfile() {
        assertThrows(IllegalArgumentException.class, () -> RemoteUserProfile.parse("uid-1", null));
    }
    @Test public void rejectsUidMismatch() {
        assertThrows(IllegalArgumentException.class, () -> RemoteUserProfile.parse("other", profile()));
    }
    @Test public void rejectsUnknownOrMissingRole() {
        Map<String, Object> data = profile(); data.put("role", "Administrador");
        assertThrows(IllegalArgumentException.class, () -> RemoteUserProfile.parse("uid-1", data));
        data.remove("role");
        assertThrows(IllegalArgumentException.class, () -> RemoteUserProfile.parse("uid-1", data));
    }
    @Test public void rejectsStringActiveAndEmptyName() {
        Map<String, Object> data = profile(); data.put("active", "true");
        assertThrows(IllegalArgumentException.class, () -> RemoteUserProfile.parse("uid-1", data));
        data.put("active", true); data.put("name", " ");
        assertThrows(IllegalArgumentException.class, () -> RemoteUserProfile.parse("uid-1", data));
    }
}
