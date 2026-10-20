package com.example.proyecto_iotelito;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.TextView;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.data.superadmin.local.AdminHotelEntity;
import com.example.proyecto_iotelito.data.superadmin.local.SuperadminDatabase;
import com.example.proyecto_iotelito.ui.hoteladmin.hotel.DatosHotelActivity;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class HotelAdminSharedProfileUiTest {
    @Test public void datosAdminMuestraElPerfilDeLaBaseCompartida() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context context = instrumentation.getTargetContext();
        Intent intent = new Intent(context, DatosHotelActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Activity activity = instrumentation.startActivitySync(intent);
        try {
            long deadline = SystemClock.uptimeMillis() + 10000;
            boolean[] loaded = {false};
            while (!loaded[0] && SystemClock.uptimeMillis() < deadline) {
                instrumentation.runOnMainSync(() ->
                        loaded[0] = activity.findViewById(R.id.btn_guardar).isEnabled());
                if (!loaded[0]) SystemClock.sleep(50);
            }
            assertTrue("Admin no cargó el perfil compartido", loaded[0]);
            AdminHotelEntity shared = SuperadminDatabase.getInstance(context).dao()
                    .hotel(HotelAdminSampleData.HOTEL_ID);
            assertNotNull(shared);
            instrumentation.runOnMainSync(() -> {
                assertEquals(shared.name, ((TextView) activity.findViewById(R.id.et_nombre)).getText().toString());
                assertEquals(shared.address, ((TextView) activity.findViewById(R.id.et_direccion)).getText().toString());
                assertEquals(shared.attractions, ((TextView) activity.findViewById(R.id.et_atracciones)).getText().toString());
            });
        } finally {
            instrumentation.runOnMainSync(activity::finish);
        }
    }
}
