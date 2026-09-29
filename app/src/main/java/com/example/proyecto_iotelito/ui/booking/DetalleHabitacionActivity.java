package com.example.proyecto_iotelito.ui.booking;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelMedia;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;

import java.util.Locale;

/**
 * Detalle de la habitación destacada de un hotel. Muestra la selección
 * realizada en Explorar y calcula el total según sus noches reales.
 */
public class DetalleHabitacionActivity extends AppCompatActivity {

    public static final String EXTRA_HOTEL_ID = "extra_hotel_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_habitacion);

        int hotelId = getIntent().getIntExtra(EXTRA_HOTEL_ID, 1);
        Hotel hotel = SampleData.findById(hotelId);
        BookingSelection bookingSelection = BookingSelection.from(getIntent());

        ((ImageView) findViewById(R.id.iv_room_photo))
                .setImageResource(HotelMedia.galleryImage(hotel.id, 1));

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(hotel.roomName);

        ((TextView) findViewById(R.id.tv_room_name)).setText(hotel.roomName);
        ((TextView) findViewById(R.id.tv_room_price)).setText(
                String.format(Locale.getDefault(), "S/ %.0f", hotel.pricePerNight));
        // Esta etiqueta describe la reserva que el usuario está armando, no la
        // capacidad estática usada originalmente por el mockup.
        ((TextView) findViewById(R.id.tag_capacidad)).setText(
                bookingSelection.textoHuespedes(this));
        ((TextView) findViewById(R.id.tag_tamano)).setText(hotel.roomSize);
        ((TextView) findViewById(R.id.tv_estadia)).setText(
                getString(R.string.estadia_seleccionada, bookingSelection.textoFechas()));

        poblarCaracteristicas(hotel);

        double total = hotel.pricePerNight * bookingSelection.noches();
        findViewById(R.id.btn_reservar).setOnClickListener(v -> {
            Intent intent = new Intent(this, ConfirmacionReservaActivity.class);
            intent.putExtra(ConfirmacionReservaActivity.EXTRA_HOTEL_ID, hotel.id);
            intent.putExtra(ConfirmacionReservaActivity.EXTRA_TOTAL, total);
            bookingSelection.putInto(intent);
            startActivity(intent);
        });
    }

    private void poblarCaracteristicas(Hotel hotel) {
        LinearLayout container = findViewById(R.id.features_list);
        container.removeAllViews();
        for (String feature : hotel.roomFeatures) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(4), 0, dp(4));

            ImageView check = new ImageView(this);
            LinearLayout.LayoutParams checkParams = new LinearLayout.LayoutParams(dp(16), dp(16));
            check.setLayoutParams(checkParams);
            check.setImageResource(R.drawable.ic_check);
            check.setImageTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.io_teal)));
            row.addView(check);

            TextView label = new TextView(this);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.setMarginStart(dp(8));
            label.setLayoutParams(labelParams);
            label.setText(feature);
            label.setTextSize(13);
            label.setTextColor(ContextCompat.getColor(this, R.color.io_text_secondary));
            row.addView(label);

            container.addView(row);
        }
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(value * density);
    }
}
