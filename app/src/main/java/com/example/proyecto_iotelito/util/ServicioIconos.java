package com.example.proyecto_iotelito.util;

import com.example.proyecto_iotelito.R;

import java.util.Locale;

/**
 * Mapea el texto de un servicio de hotel (ver {@link com.example.proyecto_iotelito.data.SampleData})
 * a un ícono representativo, para los chips de filtro por servicios.
 */
public final class ServicioIconos {

    private ServicioIconos() {
    }

    public static int iconoPara(String servicio) {
        String texto = servicio.toLowerCase(Locale.getDefault());
        if (texto.contains("wifi") || texto.contains("wi-fi")) {
            return R.drawable.ic_wifi;
        }
        if (texto.contains("desayuno")) {
            return R.drawable.ic_restaurant;
        }
        if (texto.contains("piscina")) {
            return R.drawable.ic_waves;
        }
        if (texto.contains("estacionamiento") || texto.contains("parking")) {
            return R.drawable.ic_local_parking;
        }
        if (texto.contains("oxigen")) {
            return R.drawable.ic_bolt;
        }
        if (texto.contains("café") || texto.contains("cafe")) {
            return R.drawable.ic_local_cafe;
        }
        return R.drawable.ic_check_circle;
    }
}
