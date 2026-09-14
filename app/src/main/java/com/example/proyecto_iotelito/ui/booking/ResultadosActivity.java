package com.example.proyecto_iotelito.ui.booking;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelMedia;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Lista de hoteles resultado de una búsqueda (datos estáticos).
 * Demuestra: Intent con extras, PopupMenu (Clase 2.3 - Menús) para
 * ordenar por precio, y Chips seleccionables de Material Design.
 */
public class ResultadosActivity extends AppCompatActivity {

    public static final String EXTRA_DESTINO = "extra_destino";
    public static final String EXTRA_RESUMEN = "extra_resumen";

    private final List<Hotel> hoteles = new ArrayList<>(SampleData.HOTELS);
    private LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resultados);

        String destino = getIntent().getStringExtra(EXTRA_DESTINO);
        if (destino == null) {
            destino = "Lima, Perú";
        }
        ((TextView) findViewById(R.id.tv_destino)).setText(destino);

        String resumen = getIntent().getStringExtra(EXTRA_RESUMEN);
        if (resumen != null) {
            ((TextView) findViewById(R.id.tv_resumen)).setText(resumen);
        }

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        listContainer = findViewById(R.id.hotel_list_container);
        renderHoteles();

        Chip chipPrecio = findViewById(R.id.chip_precio);
        View.OnClickListener mostrarMenuOrden = v -> mostrarMenuOrdenPrecio(chipPrecio);
        chipPrecio.setOnClickListener(mostrarMenuOrden);
        chipPrecio.setOnCloseIconClickListener(mostrarMenuOrden);
    }

    private void mostrarMenuOrdenPrecio(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_ordenar_precio, popupMenu.getMenu());
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.orden_menor_a_mayor) {
                Collections.sort(hoteles, Comparator.comparingDouble(h -> h.pricePerNight));
            } else if (id == R.id.orden_mayor_a_menor) {
                Collections.sort(hoteles, (a, b) -> Double.compare(b.pricePerNight, a.pricePerNight));
            }
            renderHoteles();
            Toast.makeText(this, getString(R.string.toast_orden_aplicado, item.getTitle()), Toast.LENGTH_SHORT).show();
            return true;
        });
        popupMenu.show();
    }

    private void renderHoteles() {
        listContainer.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (Hotel hotel : hoteles) {
            View card = inflater.inflate(R.layout.item_hotel_card_vertical, listContainer, false);
            bindHotelCard(card, hotel);
            if (listContainer.getChildCount() > 0) {
                ((LinearLayout.LayoutParams) card.getLayoutParams()).topMargin = dp(16);
            }
            listContainer.addView(card);
        }
    }

    private void bindHotelCard(View card, Hotel hotel) {
        ((ImageView) card.findViewById(R.id.iv_photo)).setImageResource(HotelMedia.hotelImage(hotel.id));
        ((TextView) card.findViewById(R.id.tv_location)).setText(hotel.address);
        ((TextView) card.findViewById(R.id.tv_rating)).setText(String.format(Locale.getDefault(), "%.1f", hotel.rating));
        ((TextView) card.findViewById(R.id.tv_name)).setText(hotel.name);
        ((TextView) card.findViewById(R.id.tv_price)).setText(
                getString(R.string.desde_precio, String.format(Locale.getDefault(), "S/ %.0f", hotel.pricePerNight)));

        MaterialButton btnVerDetalle = card.findViewById(R.id.btn_ver_detalle);
        btnVerDetalle.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetalleHotelActivity.class);
            intent.putExtra(DetalleHotelActivity.EXTRA_HOTEL_ID, hotel.id);
            startActivity(intent);
        });
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
