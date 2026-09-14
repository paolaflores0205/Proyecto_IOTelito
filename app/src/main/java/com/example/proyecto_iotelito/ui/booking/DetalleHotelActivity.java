package com.example.proyecto_iotelito.ui.booking;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelMedia;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Attraction;
import com.example.proyecto_iotelito.model.Hotel;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.Locale;

/**
 * Detalle de un hotel: galería, descripción, servicios (Chips) y
 * atracciones cercanas. Navega al detalle de la habitación destacada.
 */
public class DetalleHotelActivity extends AppCompatActivity {

    public static final String EXTRA_HOTEL_ID = "extra_hotel_id";

    private static final int[] SERVICE_ICONS = {
            R.drawable.ic_wifi, R.drawable.ic_local_cafe, R.drawable.ic_cancel
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_hotel);

        int hotelId = getIntent().getIntExtra(EXTRA_HOTEL_ID, 1);
        Hotel hotel = SampleData.findById(hotelId);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        findViewById(R.id.iv_favorito).setOnClickListener(v ->
                Toast.makeText(this, R.string.cd_favorito, Toast.LENGTH_SHORT).show());

        ((TextView) findViewById(R.id.tv_title)).setText(hotel.name);
        ((TextView) findViewById(R.id.tv_nombre)).setText(hotel.name);
        ((TextView) findViewById(R.id.tv_rating)).setText(String.format(Locale.getDefault(), "%.1f", hotel.rating));
        ((TextView) findViewById(R.id.tv_opiniones)).setText(getString(R.string.opiniones, hotel.reviews));
        ((TextView) findViewById(R.id.tv_direccion)).setText(hotel.address);
        ((TextView) findViewById(R.id.tv_descripcion)).setText(hotel.description);

        findViewById(R.id.card_mapa).setOnClickListener(v -> abrirUbicacionEnMapa(hotel));

        poblarGaleria(hotel);
        poblarServicios(hotel);
        poblarAtracciones(hotel);

        findViewById(R.id.btn_ver_habitaciones).setOnClickListener(v -> {
            Intent intent = new Intent(this, DetalleHabitacionActivity.class);
            intent.putExtra(DetalleHabitacionActivity.EXTRA_HOTEL_ID, hotel.id);
            startActivity(intent);
        });
    }

    private void poblarGaleria(Hotel hotel) {
        LinearLayout galleryRow = findViewById(R.id.gallery_row);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < 4; i++) {
            View photo = inflater.inflate(R.layout.item_gallery_photo, galleryRow, false);
            ((ImageView) photo.findViewById(R.id.iv_gallery_photo))
                    .setImageResource(HotelMedia.galleryImage(hotel.id, i));
            galleryRow.addView(photo);
        }
    }

    private void poblarServicios(Hotel hotel) {
        ChipGroup chipGroup = findViewById(R.id.services_grid);
        chipGroup.removeAllViews();
        for (int i = 0; i < hotel.services.length; i++) {
            chipGroup.addView(createServiceChip(hotel.services[i], SERVICE_ICONS[i % SERVICE_ICONS.length]));
        }
    }

    private Chip createServiceChip(String label, int iconRes) {
        Chip chip = new Chip(this);
        chip.setText(label);
        chip.setTextSize(12);
        chip.setTextColor(ContextCompat.getColor(this, R.color.io_text_primary));
        chip.setChipIconResource(iconRes);
        chip.setChipIconTint(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.io_teal)));
        chip.setChipBackgroundColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
        chip.setChipStrokeColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.io_border)));
        chip.setChipStrokeWidth(1f);
        chip.setClickable(false);
        chip.setCheckable(false);
        chip.setEnsureMinTouchTargetSize(false);
        return chip;
    }

    /**
     * Carrusel horizontal de tarjetas con foto (Clase 01.3 - Vector Assets +
     * Material Cards): cada atracción cercana se ve como una mini "postal"
     * con distancia superpuesta, en vez de una simple lista de texto.
     */
    private void poblarAtracciones(Hotel hotel) {
        LinearLayout container = findViewById(R.id.attractions_row);
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < hotel.attractions.length; i++) {
            Attraction attraction = hotel.attractions[i];
            View card = inflater.inflate(R.layout.item_atraccion_card, container, false);
            ((ImageView) card.findViewById(R.id.iv_foto_atraccion))
                    .setImageResource(HotelMedia.galleryImage(hotel.id, i + 1));
            ((TextView) card.findViewById(R.id.tv_distancia_atraccion)).setText(attraction.distance);
            ((TextView) card.findViewById(R.id.tv_nombre_atraccion)).setText(attraction.name);
            container.addView(card);
        }
    }

    /**
     * Abre la dirección del hotel en Google Maps (o el navegador si no hay
     * app de mapas instalada) mediante un Intent implícito: evita integrar
     * el SDK de Maps (API key, Play Services) para datos que aún son estáticos.
     */
    private void abrirUbicacionEnMapa(Hotel hotel) {
        Uri uri = Uri.parse("https://www.google.com/maps/search/?api=1&query="
                + Uri.encode(hotel.address));
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.cd_ver_mapa, Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
