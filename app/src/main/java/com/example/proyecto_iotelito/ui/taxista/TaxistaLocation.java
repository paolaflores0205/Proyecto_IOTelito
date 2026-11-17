package com.example.proyecto_iotelito.ui.taxista;

import android.Manifest;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.*;

import com.example.proyecto_iotelito.data.taxista.local.ServicioConductorEntity;
import com.example.proyecto_iotelito.ui.taxi.TaxiUi;

final class TaxistaLocation {
    private TaxistaLocation() {}

    static void bind(AppCompatActivity a, TaxistaViewModel vm) {
        final int[] active = {-1};
        android.content.SharedPreferences prefs = a.getSharedPreferences("taxi_location", 0);
        ActivityResultLauncher<String[]> permission =
                a.registerForActivityResult(
                        new ActivityResultContracts.RequestMultiplePermissions(),
                        r -> {
                            if (TaxistaLocationService.permiso(a) && active[0] > 0)
                                TaxistaLocationService.start(a, active[0]);
                            else
                                TaxiUi.status(
                                        a,
                                        "La ubicación no se comparte. Puedes habilitar el permiso"
                                            + " de ubicación en Ajustes de la app.");
                        });
        Runnable update =
                () -> {
                    if (!a.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED))
                        return;
                    if (active[0] < 1) {
                        if (vm.repo.listaVerificada) TaxistaLocationService.stop(a);
                        return;
                    }
                    if (TaxistaLocationService.permiso(a)) {
                        try {
                            TaxistaLocationService.start(a, active[0]);
                        } catch (RuntimeException e) {
                            TaxiUi.status(
                                    a,
                                    "No se pudo iniciar el envío de ubicación. Vuelve a abrir el"
                                        + " servicio.");
                        }
                    } else if (!prefs.getBoolean("asked", false)) {
                        prefs.edit().putBoolean("asked", true).apply();
                        permission.launch(
                                new String[] {
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                });
                    }
                };
        vm.repo.servicios.observe(
                a,
                rows -> {
                    active[0] = -1;
                    for (ServicioConductorEntity e : rows)
                        if ("ASIGNADO".equals(e.estado)
                                || "EN_CAMINO".equals(e.estado)
                                || "EN_TRASLADO".equals(e.estado)) {
                            active[0] = e.id;
                            break;
                        }
                    update.run();
                });
        a.getLifecycle()
                .addObserver(
                        new DefaultLifecycleObserver() {
                            public void onResume(LifecycleOwner owner) {
                                update.run();
                            }
                        });
    }
}
