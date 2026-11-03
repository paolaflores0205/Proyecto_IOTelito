package com.example.proyecto_iotelito.ui.hoteladmin;

import android.app.Activity;
import android.content.Intent;
import android.widget.Toast;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository;
import com.example.proyecto_iotelito.model.auth.*;
import com.example.proyecto_iotelito.ui.auth.LoginActivity;

/** Rechaza acceso por intents sin una sesión admin; conserva fixtures locales explícitos. */
public final class HotelAdminAccessGuard {
    private HotelAdminAccessGuard() { }
    public static boolean allow(Activity activity) {
        AuthenticatedUser user = SessionRepository.obtener(activity);
        boolean remote = FirebaseHotelAdminRepository.remote(activity);
        if (user == null || user.getRole() != UserRole.HOTEL_ADMIN
                || (remote && (user.getUid() == null || user.getHotelId() == null))) {
            SessionRepository.cerrar(activity);
            activity.startActivity(new Intent(activity, LoginActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
            activity.finish(); return false;
        }
        String name = activity.getClass().getName();
        if (remote && !name.equals(HotelAdminMainActivity.class.getName()) && !name.contains(".hoteladmin.hotel.")) {
            Toast.makeText(activity, com.example.proyecto_iotelito.R.string.hoteladmin_seccion_pendiente, Toast.LENGTH_LONG).show();
            activity.startActivity(new Intent(activity, HotelAdminMainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
            activity.finish(); return false;
        }
        return true;
    }
    public static void verify(Activity activity) {
        if (activity.getClass().getName().startsWith("com.example.proyecto_iotelito.ui.hoteladmin.")) allow(activity);
    }
}
