package com.example.proyecto_iotelito.ui.reservas;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.NotificacionRepository;
import com.example.proyecto_iotelito.data.ReservaRepository;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.data.TaxiRepository;
import com.example.proyecto_iotelito.ui.taxi.TaxistaAsignadoActivity;
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
    public static final String EXTRA_NOTIFICACION_ID = "extra_notificacion_id";
    private Hotel hotelMostrado;
    private Reserva reservaMostrada;
    private int solicitudTaxi=-1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_reserva);
        int reservaId=getIntent().getIntExtra(EXTRA_RESERVA_ID,-1);
        findViewById(R.id.btn_estado_taxi_reserva).setVisibility(View.VISIBLE);
        findViewById(R.id.btn_estado_taxi_reserva).setOnClickListener(v->{
            Intent intent=solicitudTaxi>=0?new Intent(this,TaxistaAsignadoActivity.class).putExtra(TaxiRepository.EXTRA_ID,solicitudTaxi)
                :reservaMostrada!=null&&com.example.proyecto_iotelito.data.TaxiBenefitPolicy.puedeSolicitar(reservaMostrada,hotelMostrado)
                ?new Intent(this,com.example.proyecto_iotelito.ui.taxi.BeneficioTaxiActivity.class).putExtra(com.example.proyecto_iotelito.ui.taxi.BeneficioTaxiActivity.EXTRA_RESERVA_ID,reservaId)
                :new Intent(this,com.example.proyecto_iotelito.ui.taxi.MisTaxisActivity.class);
            startActivity(intent);
        });
        if (reservaId > 0) com.example.proyecto_iotelito.data.taxiapi.TaxiTasks.poll(this, 5000,
                () -> TaxiRepository.porReserva(this, reservaId),
                solicitud -> { solicitudTaxi = solicitud; actualizarTaxi(); },
                error -> { solicitudTaxi = -1; actualizarTaxi(); });
        ReservaRepository.getInstance(this).observarTodas().observe(this,rows->{
            if(hotelMostrado==null)return;
            for(Reserva row:rows)if(row.id==reservaId){pintarReserva(row,hotelMostrado);break;}
        });

        int notificacionId = getIntent().getIntExtra(EXTRA_NOTIFICACION_ID, -1);
        if (notificacionId != -1) {
            NotificacionRepository.getInstance(this).marcarLeida(notificacionId);
        }
    }

    private void pintarReserva(Reserva reserva, Hotel hotel) {
        hotelMostrado=hotel;reservaMostrada=reserva;actualizarTaxi();
        if (getIntent().getBooleanExtra(com.example.proyecto_iotelito.notification.ClientCheckoutAlerts.EXTRA_CHECKOUT_ALERT, false)) {
            com.example.proyecto_iotelito.notification.ClientCheckoutAlerts.markRead(this, reserva.id);
        }
        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(hotel.name);

        boolean enCurso = reserva.esActiva() && reserva.estado != Reserva.Estado.EN_CHECKOUT && reserva.estaEnCurso();
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
        pintarImportes(reserva);

        View btnCheckout = findViewById(R.id.btn_realizar_checkout);
        View btnVerHotel = findViewById(R.id.btn_ver_hotel);

        btnCheckout.setVisibility(puedeSolicitarSalida(reserva) ? View.VISIBLE : View.GONE);
        btnCheckout.setOnClickListener(v -> {
            Intent intent = new Intent(this, CheckoutActivity.class);
            intent.putExtra(CheckoutActivity.EXTRA_RESERVA_ID, reserva.id);
            startActivity(intent);
        });

        View comprobante = findViewById(R.id.btn_ver_comprobante);
        comprobante.setVisibility(reserva.estado == Reserva.Estado.EN_CHECKOUT
                || com.example.proyecto_iotelito.data.CheckoutPolicy.finalizada(reserva.estado.name()) ? View.VISIBLE : View.GONE);
        ((TextView)comprobante).setText(reserva.estado == Reserva.Estado.EN_CHECKOUT ? "Ver estado del checkout" : "Ver comprobante");
        comprobante.setOnClickListener(v -> startActivity(new Intent(this, com.example.proyecto_iotelito.ui.pago.CobroConfirmadoActivity.class)
                .putExtra(com.example.proyecto_iotelito.ui.pago.CobroConfirmadoActivity.EXTRA_RESERVA_ID, reserva.id)));
        findViewById(R.id.btn_chatear).setVisibility(reserva.esActiva() ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_chatear).setOnClickListener(v -> {
            Intent intent = new Intent(this, ChatHotelActivity.class);
            intent.putExtra(ChatHotelActivity.EXTRA_HOTEL_ID, hotel.id);
            intent.putExtra(ChatHotelActivity.EXTRA_RESERVA_ID, reserva.id);
            startActivity(intent);
        });

        btnVerHotel.setVisibility(reserva.estado == Reserva.Estado.CANCELADA ? View.GONE : View.VISIBLE);
        btnVerHotel.setOnClickListener(v -> {
            Intent intent = new Intent(this, DetalleHotelActivity.class);
            intent.putExtra(DetalleHotelActivity.EXTRA_HOTEL_ID, hotel.id);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        View checkout = findViewById(R.id.btn_realizar_checkout);
        checkout.setVisibility(View.GONE);
        int id = getIntent().getIntExtra(EXTRA_RESERVA_ID, -1);
        com.example.proyecto_iotelito.data.ClientDataLoader.reserva(this, id, this::pintarReserva);
    }

    private void actualizarTaxi(){
        ((TextView)findViewById(R.id.btn_estado_taxi_reserva)).setText(solicitudTaxi>=0?"Seguir mi taxi":reservaMostrada!=null&&com.example.proyecto_iotelito.data.TaxiBenefitPolicy.puedeSolicitar(reservaMostrada,hotelMostrado)?"Solicitar taxi de cortesía":"Mis solicitudes de taxi");
    }

    private void pintarImportes(Reserva reserva){
        com.example.proyecto_iotelito.data.ComprobanteCheckout receipt=reserva.comprobante;
        boolean finalized=com.example.proyecto_iotelito.data.CheckoutPolicy.finalizada(reserva.estado.name());
        bindRow(R.id.row_alojamiento_detalle,R.string.label_alojamiento_reserva,formatMoney(reserva.precioTotal));
        View extras=findViewById(R.id.row_adicionales_detalle);
        TextView note=findViewById(R.id.tv_motivo_adicional_detalle);
        extras.setVisibility(receipt!=null||finalized||reserva.estado==Reserva.Estado.EN_CHECKOUT?View.VISIBLE:View.GONE);
        bindRow(R.id.row_adicionales_detalle,R.string.label_adicionales_reserva,
                receipt!=null?formatMoney(receipt.extra):"Por confirmar");
        ((TextView)findViewById(R.id.tv_label_monto_total)).setText(receipt!=null?"Total cobrado (simulado)":finalized?"Total del cobro":"Total de alojamiento");
        ((TextView)findViewById(R.id.tv_monto_total)).setText(finalized&&receipt==null?"Por consultar":formatMoney(reserva.importeMostrado()));
        note.setVisibility(finalized||receipt!=null?View.VISIBLE:View.GONE);
        note.setText(receipt==null?"No se pudo confirmar el comprobante. Vuelve a consultar con conexión."
                :receipt.extra>0?"Motivo: "+receipt.motivo+"\nObservación: "+receipt.observacion:"Sin cargos adicionales.");
    }

    private String mensajeEstado(Reserva reserva) {
        if (reserva.estado == Reserva.Estado.EN_CHECKOUT) return getString(R.string.checkout_pendiente_cliente);
        if (reserva.esActiva() && reserva.puedeHacerCheckout()) {
            return getString(R.string.mensaje_puede_checkout);
        }
        if (reserva.esActiva() && reserva.estado != Reserva.Estado.EN_CHECKOUT && reserva.estaEnCurso()) {
            return getString(R.string.mensaje_estado_en_curso);
        }
        return getString(EstadoUi.mensaje(reserva.estado));
    }

    private boolean puedeSolicitarSalida(Reserva reserva) {
        return com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? reserva.puedeSolicitarCheckout() : reserva.puedeHacerCheckout();
    }

    private void bindRow(int rowId, int labelRes, String value) {
        View row = findViewById(rowId);
        ((TextView) row.findViewById(R.id.tv_label)).setText(labelRes);
        ((TextView) row.findViewById(R.id.tv_value)).setText(value);
    }

    private String formatMoney(double amount) {
        return "S/ " + String.format(Locale.US, "%,.2f", amount);
    }
}
