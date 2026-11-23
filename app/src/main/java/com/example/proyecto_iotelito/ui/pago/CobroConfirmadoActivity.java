package com.example.proyecto_iotelito.ui.pago;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.proyecto_iotelito.MainActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.*;
import com.example.proyecto_iotelito.model.Reserva;
import java.util.Locale;

/** Muestra la solicitud pendiente; solo un comprobante del servidor acredita el cobro simulado. */
public class CobroConfirmadoActivity extends AppCompatActivity {
    public static final String EXTRA_RESERVA_ID = "extra_reserva_id";
    private Reserva reserva;
    private int version;
    private String estadoObservado;
    private boolean comprobanteMostrado;
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_cobro_confirmado);
        ((android.widget.ImageView)findViewById(R.id.iv_estado_cobro)).setImageResource(R.drawable.ic_info_outline);
        findViewById(R.id.btn_continuar).setOnClickListener(v -> irAlShell());
        findViewById(R.id.btn_ver_historial).setOnClickListener(v -> irAlShell());
        findViewById(R.id.btn_reintentar_cobro).setOnClickListener(v -> cargar());
        findViewById(R.id.btn_taxi_checkout).setOnClickListener(v -> abrirTaxi());
        cargar();
        ReservaRepository.getInstance(this).observarTodas().observe(this, rows -> {
            for (Reserva row : rows) if (row.id == getIntent().getIntExtra(EXTRA_RESERVA_ID, -1)) {
                if (!row.estado.name().equals(estadoObservado) || (row.comprobante!=null&&!comprobanteMostrado)) cargar();
                estadoObservado = row.estado.name();
                break;
            }
        });
    }
    private void cargar() {
        final int request = ++version;
        findViewById(R.id.btn_reintentar_cobro).setVisibility(View.GONE);
        if (!ConexionCliente.disponible(this)) {
            text(R.id.tv_titulo_cobro, "Sin conexión"); text(R.id.tv_estado_cobro, getString(R.string.cliente_sin_conexion));
            findViewById(R.id.btn_reintentar_cobro).setVisibility(View.VISIBLE); return;
        }
        int id = getIntent().getIntExtra(EXTRA_RESERVA_ID, -1);
        ClientLocalTasks.run(this, this, () -> {
            Reserva row = ReservaRepository.getInstance(this).buscarPorId(id);
            if (row == null) throw new IllegalArgumentException("No se encontró tu reserva.");
            com.example.proyecto_iotelito.model.Hotel hotel = HotelCatalogRepository.buscar(this, row.hotelId);
            if (hotel == null) throw new IllegalArgumentException("No se encontró el hotel.");
            ComprobanteCheckout receipt = row.comprobante;
            return new Object[]{row, hotel.name, receipt, hotel};
        }, data -> {
            if (request != version) return;
            reserva = (Reserva)data[0];
            estadoObservado = reserva.estado.name();
            ComprobanteCheckout receipt = (ComprobanteCheckout)data[2];
            comprobanteMostrado=receipt!=null;
            bind(R.id.row_hotel, "Hotel", (String)data[1]);
            bind(R.id.row_habitacion, "Habitación", reserva.roomName + " · " + reserva.roomNumber);
            bind(R.id.row_fechas, "Fechas", reserva.rangoFechasTexto());
            bind(R.id.row_base_checkout, "Alojamiento", money(receipt==null?reserva.precioTotal:receipt.base));
            bind(R.id.row_valoracion_checkout, "Tu valoración", reserva.valoracion == null ? "No registrada" : reserva.valoracion + "/5 estrellas");
            bind(R.id.row_observaciones_checkout, "Tus observaciones", reserva.observaciones == null ? "No registradas" : reserva.observaciones);
            bind(R.id.row_tarjeta, "Tarjeta de prueba", reserva.tarjeta == null ? "No registrada · Reserva anterior" : reserva.tarjeta.etiqueta());
            boolean pending = reserva.estado == Reserva.Estado.EN_CHECKOUT;
            findViewById(R.id.btn_taxi_checkout).setVisibility(TaxiBenefitPolicy.puedeSolicitar(reserva,(com.example.proyecto_iotelito.model.Hotel)data[3])?View.VISIBLE:View.GONE);
            if (receipt == null) {
                findViewById(R.id.btn_reintentar_cobro).setVisibility(pending?View.GONE:View.VISIBLE);
                text(R.id.tv_titulo_cobro, pending ? "Checkout solicitado" : "Sin comprobante de cobro");
                text(R.id.tv_estado_cobro, pending ? getString(R.string.checkout_pendiente_cliente)
                        : "No hay un comprobante administrativo confirmado para esta reserva. No se acredita ningún cobro.");
                text(R.id.tv_monto_total, "Por confirmar");
                text(R.id.tv_cargos_adicionales, "Por confirmar");
                text(R.id.tv_motivo_cobro, "Los cargos adicionales se mostrarán después de la revisión del hotel.");
                text(R.id.tv_datos_comprobante, "Demostración sin dinero real. Puedes volver a consultar desde el detalle de tu reserva.");
                return;
            }
            ((android.widget.ImageView)findViewById(R.id.iv_estado_cobro)).setImageResource(R.drawable.ic_check_circle);
            text(R.id.tv_titulo_cobro, "Cobro simulado confirmado");
            text(R.id.tv_estado_cobro, "El administrador registró el cobro de demostración. No se debitó dinero.");
            text(R.id.tv_monto_total, money(receipt.total));
            text(R.id.tv_cargos_adicionales, money(receipt.adicionales()));
            text(R.id.tv_motivo_cobro, receipt.detalleAdicionales());
            bind(R.id.row_tarjeta, "Tarjeta de prueba", receipt.tarjeta);
            String fecha = java.time.Instant.ofEpochMilli(receipt.fecha).atZone(java.time.ZoneId.of("America/Lima"))
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            text(R.id.tv_datos_comprobante, "Comprobante DEMO-" + reserva.id + "\nFecha: " + fecha
                    + "\nRegistrado por el administrador: " + receipt.administrador + "\nOperación simulada. No es un comprobante tributario.");
            ClientCheckoutNotifications.notificarUnaVez(this, reserva, receipt);
        }, () -> {
            if (request != version) return;
            text(R.id.tv_titulo_cobro, "No se pudo consultar");
            text(R.id.tv_estado_cobro, ConexionCliente.disponible(this)
                    ? "No se confirmó el comprobante. Reintenta y revisa los permisos de Firebase." : getString(R.string.cliente_sin_conexion));
            findViewById(R.id.btn_reintentar_cobro).setVisibility(View.VISIBLE);
        });
    }
    private void abrirTaxi() {
        if (reserva == null) return;
        TaxiRepository.ejecutar(this, this, () -> TaxiRepository.porReserva(this, reserva.id), id -> {
            Intent intent = id >= 0 ? new Intent(this, com.example.proyecto_iotelito.ui.taxi.TaxistaAsignadoActivity.class)
                    .putExtra(TaxiRepository.EXTRA_ID, id) : new Intent(this, com.example.proyecto_iotelito.ui.taxi.BeneficioTaxiActivity.class)
                    .putExtra(com.example.proyecto_iotelito.ui.taxi.BeneficioTaxiActivity.EXTRA_RESERVA_ID, reserva.id);
            startActivity(intent);
        });
    }
    private void bind(int id, String label, String value) {
        View row = findViewById(id); ((TextView)row.findViewById(R.id.tv_label)).setText(label);
        ((TextView)row.findViewById(R.id.tv_value)).setText(value);
    }
    private void text(int id, String value) { ((TextView)findViewById(id)).setText(value); }
    private String money(double value) { return String.format(Locale.US, "S/ %,.2f", value); }
    private void irAlShell() {
        startActivity(new Intent(this, MainActivity.class).putExtra(MainActivity.EXTRA_OPEN_TAB, R.id.nav_reservas)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP)); finish();
    }
}
