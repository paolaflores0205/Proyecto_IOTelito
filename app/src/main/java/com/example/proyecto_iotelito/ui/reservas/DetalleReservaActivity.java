package com.example.proyecto_iotelito.ui.reservas;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.Hotel;
import com.example.proyecto_iotelito.model.Reserva;
import com.example.proyecto_iotelito.ui.booking.DetalleHotelActivity;
import com.example.proyecto_iotelito.ui.pago.CheckoutActivity;

import java.util.Locale;

/**
 * Detalle de una reserva: estado, resumen y acciones (chatear, ver hotel
 * y, solo el último día de una estadía en curso, realizar el checkout).
 */
public class DetalleReservaActivity extends AppCompatActivity {

    public static final String EXTRA_RESERVA_ID = "extra_reserva_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_reserva);

        int reservaId = getIntent().getIntExtra(EXTRA_RESERVA_ID, SampleData.RESERVAS.get(0).id);
        Reserva reserva = SampleData.findReservaById(reservaId);
        Hotel hotel = SampleData.findById(reserva.hotelId);

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(hotel.name);

        boolean enCurso = reserva.esActiva() && reserva.estaEnCurso();
        TextView tvEstadoPill = findViewById(R.id.tv_estado_pill);
        int colorFondo = enCurso ? R.color.io_teal : EstadoUi.colorFondo(reserva.estado);
        int colorTexto = enCurso ? R.color.white : EstadoUi.colorTexto(reserva.estado);
        tvEstadoPill.setText(enCurso ? getString(R.string.estado_en_curso) : EstadoUi.texto(this, reserva.estado));
        tvEstadoPill.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, colorFondo)));
        tvEstadoPill.setTextColor(ContextCompat.getColor(this, colorTexto));
        ((TextView) findViewById(R.id.tv_mensaje_estado)).setText(mensajeEstado(reserva));

        ((TextView) findViewById(R.id.tv_hotel_nombre)).setText(hotel.name);
        bindRow(R.id.row_habitacion, R.string.label_habitacion,
                getString(R.string.habitacion_con_numero, reserva.roomName, reserva.roomNumber));
        bindRow(R.id.row_fechas, R.string.label_fechas, reserva.rangoFechasTexto());
        bindRow(R.id.row_noches, R.string.label_noches,
                getResources().getQuantityString(R.plurals.noches_plural, reserva.noches, reserva.noches));
        bindRow(R.id.row_huespedes, R.string.label_huespedes_dp, reserva.huespedes);
        bindRow(R.id.row_codigo, R.string.label_codigo, reserva.codigo);
        ((TextView) findViewById(R.id.tv_monto_total)).setText(formatMoney(reserva.precioTotal));

        View btnCheckout = findViewById(R.id.btn_realizar_checkout);
        View btnVerHotel = findViewById(R.id.btn_ver_hotel);

        btnCheckout.setVisibility(reserva.puedeHacerCheckout() ? View.VISIBLE : View.GONE);
        btnCheckout.setOnClickListener(v -> {
            Intent intent = new Intent(this, CheckoutActivity.class);
            intent.putExtra(CheckoutActivity.EXTRA_RESERVA_ID, reserva.id);
            startActivity(intent);
        });

        findViewById(R.id.btn_chatear).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatHotelActivity.class);
            intent.putExtra(ChatHotelActivity.EXTRA_HOTEL_ID, hotel.id);
            startActivity(intent);
        });

        btnVerHotel.setVisibility(reserva.estado == Reserva.Estado.CANCELADA ? View.GONE : View.VISIBLE);
        btnVerHotel.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetalleHotelActivity.class);
            intent.putExtra(DetalleHotelActivity.EXTRA_HOTEL_ID, hotel.id);
            startActivity(intent);
        });
    }

    private String mensajeEstado(Reserva reserva) {
        if (reserva.esActiva() && reserva.puedeHacerCheckout()) {
            return getString(R.string.mensaje_puede_checkout);
        }
        if (reserva.esActiva() && reserva.estaEnCurso()) {
            return getString(R.string.mensaje_estado_en_curso);
        }
        return getString(EstadoUi.mensaje(reserva.estado));
    }

    private void bindRow(int rowId, int labelRes, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tv_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.tv_value)).setText(value);
    }

    private String formatMoney(double amount) {
        return "S/ " + String.format(Locale.US, "%,.0f", amount);
    }
}
