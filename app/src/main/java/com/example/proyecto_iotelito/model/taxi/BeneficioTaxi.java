package com.example.proyecto_iotelito.model.taxi;

import androidx.annotation.DrawableRes;

public class BeneficioTaxi {

    private final String titulo;
    private final int iconResId;

    public BeneficioTaxi(String titulo, @DrawableRes int iconResId) {
        this.titulo = titulo;
        this.iconResId = iconResId;
    }

    public String getTitulo() {
        return titulo;
    }

    public int getIconResId() {
        return iconResId;
    }
}
