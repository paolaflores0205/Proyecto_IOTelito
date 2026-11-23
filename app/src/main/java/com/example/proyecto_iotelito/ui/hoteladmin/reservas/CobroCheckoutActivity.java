package com.example.proyecto_iotelito.ui.hoteladmin.reservas;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.data.hoteladmin.HotelAdminLocalStore;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminCobroCheckoutBinding;
import com.example.proyecto_iotelito.databinding.ItemHoteladminConsumoBinding;
import com.example.proyecto_iotelito.model.hoteladmin.ItemConsumo;
import com.example.proyecto_iotelito.model.hoteladmin.ReservaAdmin;
import com.example.proyecto_iotelito.ui.hoteladmin.HotelAdminNotifications;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

/** Pantalla adm-cobro-checkout. Persiste el cobro y avisa con una notificación local. */
public class CobroCheckoutActivity extends AppCompatActivity {
    public static final String EXTRA_RESERVA_ID = "reserva_id";

    private ActivityHoteladminCobroCheckoutBinding binding;
    private ReservaAdmin reserva;
    private com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelOperations remote;
    /** Catálogo activo del hotel y cuántas veces consumió el huésped cada servicio (id → cantidad). */
    private final java.util.List<com.example.proyecto_iotelito.model.hoteladmin.Servicio> catalogo = new java.util.ArrayList<>();
    private final java.util.Map<Integer, Integer> cantidades = new java.util.HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHoteladminCobroCheckoutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        if (!com.example.proyecto_iotelito.ui.hoteladmin.HotelAdminAccessGuard.allow(this)) return;
        boolean online = com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository.remote(this);
        if (online) remote = new com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelOperations(this);
        else reserva = HotelAdminSampleData.reserva(getIntent().getIntExtra(EXTRA_RESERVA_ID, 101));

        binding.toolbar.tvTitle.setText(R.string.hoteladmin_checkout_titulo);
        binding.toolbar.ivBack.setOnClickListener(v -> finish());
        binding.switchDanos.setOnCheckedChangeListener((button, checked) -> {
            binding.llDanos.setVisibility(checked ? View.VISIBLE : View.GONE);
            actualizarTotal();
        });
        binding.etMontoDano.addTextChangedListener(new SimpleTextWatcher(this::actualizarTotal));
        binding.btnConfirmar.setOnClickListener(v -> validarYConfirmar());
        binding.btnConfirmar.setEnabled(false);
        if (remote != null) {
            binding.tvAviso.setText("Cierre administrativo. No realiza cargos bancarios.");
            binding.btnConfirmar.setText("Confirmar cobro simulado");
            remote.load(getIntent().getIntExtra(EXTRA_RESERVA_ID, -1), row -> {
                if (isFinishing() || isDestroyed()) return;
                reserva = row; mostrarReserva(); binding.btnConfirmar.setEnabled(true);
                if (reserva.estado == ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE) cargarServicios();
            }, error -> { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); finish(); });
        } else { mostrarReserva(); binding.btnConfirmar.setEnabled(true); }
    }

    private void mostrarReserva() {
        binding.tvHuesped.setText(reserva.huespedNombre);
        binding.tvDocumento.setText(reserva.huespedDoc);
        binding.tvHabitacion.setText(reserva.habitacion);
        binding.tvFechas.setText(getString(R.string.hoteladmin_checkout_fechas,
                reserva.rangoFechas, reserva.noches));
        binding.tvAlojamiento.setText(moneda(reserva.precioAlojamiento));
        binding.llConsumos.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (ItemConsumo consumo : reserva.consumos) {
            ItemHoteladminConsumoBinding item = ItemHoteladminConsumoBinding.inflate(
                    inflater, binding.llConsumos, false);
            item.tvConcepto.setText(consumo.concepto);
            item.tvMonto.setText(moneda(consumo.monto));
            binding.llConsumos.addView(item.getRoot());
        }
        binding.tvSinConsumos.setVisibility(reserva.consumos.isEmpty() ? View.VISIBLE : View.GONE);
        binding.tvSubtotal.setText(moneda(reserva.total()));
        boolean puedeCobrar = reserva.estado == ReservaAdmin.EstadoCheckout.CHECKOUT_PENDIENTE;
        binding.btnConfirmar.setVisibility(puedeCobrar ? View.VISIBLE : View.GONE);
        binding.switchDanos.setVisibility(puedeCobrar ? View.VISIBLE : View.GONE);
        if (!puedeCobrar) binding.tvAviso.setText(R.string.hoteladmin_checkout_solo_lectura);
        if (remote != null && reserva.estado == ReservaAdmin.EstadoCheckout.FINALIZADA)
            binding.tvAviso.setText(reserva.tieneComprobante ? "Cierre registrado. No se realizó un cargo bancario."
                    : "Reserva finalizada sin comprobante de cierre administrativo.");
        actualizarTotal();
    }

    private void cargarServicios() {
        binding.llSeccionServicios.setVisibility(View.VISIBLE);
        remote.servicios(lista -> {
            if (isFinishing() || isDestroyed()) return;
            catalogo.clear(); catalogo.addAll(lista);
            if (catalogo.isEmpty()) binding.tvServiciosAyuda.setText(R.string.hoteladmin_servicios_sin_catalogo);
            pintarServiciosConsumibles();
        }, error -> { if (!isFinishing()) binding.tvServiciosAyuda.setText(error); });
    }

    /** Una fila por servicio del catálogo: nombre y precio, con botones − / + para la cantidad. */
    private void pintarServiciosConsumibles() {
        binding.llServiciosConsumidos.removeAllViews();
        int pad = Math.round(4 * getResources().getDisplayMetrics().density);
        for (com.example.proyecto_iotelito.model.hoteladmin.Servicio servicio : catalogo) {
            android.widget.LinearLayout fila = new android.widget.LinearLayout(this);
            fila.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            fila.setGravity(android.view.Gravity.CENTER_VERTICAL);
            fila.setPadding(0, pad, 0, pad);
            android.widget.TextView info = new android.widget.TextView(this);
            info.setText(servicio.nombre + System.lineSeparator()
                    + getString(R.string.hoteladmin_servicio_precio, moneda(servicio.precio), servicio.unidad));
            info.setTextColor(getColor(R.color.io_text_primary));
            fila.addView(info, new android.widget.LinearLayout.LayoutParams(0, android.view.ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            android.widget.TextView cantidad = new android.widget.TextView(this);
            cantidad.setGravity(android.view.Gravity.CENTER);
            cantidad.setTextColor(getColor(R.color.io_text_primary));
            cantidad.setText(String.valueOf(cantidades.getOrDefault(servicio.id, 0)));
            fila.addView(boton("−", v -> cambiarCantidad(servicio, -1, cantidad)));
            fila.addView(cantidad, new android.widget.LinearLayout.LayoutParams(pad * 9, android.view.ViewGroup.LayoutParams.WRAP_CONTENT));
            fila.addView(boton("+", v -> cambiarCantidad(servicio, 1, cantidad)));
            binding.llServiciosConsumidos.addView(fila);
        }
    }

    private View boton(String texto, View.OnClickListener click) {
        com.google.android.material.button.MaterialButton b = new com.google.android.material.button.MaterialButton(
                this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
        b.setText(texto); b.setMinWidth(0); b.setMinimumWidth(0); b.setInsetTop(0); b.setInsetBottom(0);
        b.setOnClickListener(click);
        return b;
    }

    private void cambiarCantidad(com.example.proyecto_iotelito.model.hoteladmin.Servicio servicio, int delta,
                                 android.widget.TextView vista) {
        int actual = cantidades.getOrDefault(servicio.id, 0), nueva = Math.max(0, Math.min(99, actual + delta));
        if (actual == 0 && nueva > 0 && cantidades.size() >= com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.MAX_SERVICIOS) {
            Toast.makeText(this, R.string.hoteladmin_servicios_maximo, Toast.LENGTH_SHORT).show();
            return;
        }
        if (nueva == 0) cantidades.remove(servicio.id); else cantidades.put(servicio.id, nueva);
        vista.setText(String.valueOf(nueva));
        actualizarTotal();
    }

    /** Servicios marcados, en el orden del catálogo. */
    private java.util.List<com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.ServiceLine> lineasServicios() {
        java.util.List<com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.ServiceLine> lineas = new java.util.ArrayList<>();
        for (com.example.proyecto_iotelito.model.hoteladmin.Servicio servicio : catalogo) {
            Integer cantidad = cantidades.get(servicio.id);
            if (cantidad != null && cantidad > 0) lineas.add(new com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.ServiceLine(
                    servicio.id, servicio.nombre, servicio.precio, cantidad));
        }
        return lineas;
    }

    private void actualizarTotal() {
        if (reserva == null) return;
        double total = reserva.total();
        if (remote != null) total += com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.servicesTotal(lineasServicios());
        if (binding.switchDanos.isChecked()) total += montoDano();
        binding.tvTotal.setText(moneda(total));
    }

    private void validarYConfirmar() {
        if (binding.switchDanos.isChecked()) {
            if (montoDano() <= 0) {
                com.example.proyecto_iotelito.ui.common.ErrorCampo.mostrar(binding.etMontoDano, getString(R.string.hoteladmin_error_monto_dano));
                binding.etMontoDano.requestFocus();
                return;
            }
            if (texto(binding.etMotivoDano).isEmpty()) {
                com.example.proyecto_iotelito.ui.common.ErrorCampo.mostrar(binding.etMotivoDano, getString(R.string.hoteladmin_campo_obligatorio));
                binding.etMotivoDano.requestFocus();
                return;
            }
            if (texto(binding.etObservacionDano).isEmpty()) {
                com.example.proyecto_iotelito.ui.common.ErrorCampo.mostrar(binding.etObservacionDano, getString(R.string.hoteladmin_campo_obligatorio));
                binding.etObservacionDano.requestFocus();
                return;
            }
        }
        if (remote != null) {
            try { com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.total(reserva.total(), binding.switchDanos.isChecked() ? montoDano() : 0); }
            catch (IllegalArgumentException error) { com.example.proyecto_iotelito.ui.common.ErrorCampo.mostrar(binding.etMontoDano, error.getMessage()); return; }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(remote != null ? "¿Confirmar cobro simulado?" : getString(R.string.hoteladmin_confirmar_checkout_titulo))
                .setMessage(remote != null ? "Se registrará el cierre por " + binding.tvTotal.getText() + ". No se cargará ninguna tarjeta."
                        : getString(R.string.hoteladmin_confirmar_checkout_mensaje, binding.tvTotal.getText()))
                .setNegativeButton(R.string.btn_cancelar, null)
                .setPositiveButton(R.string.hoteladmin_confirmar, (dialog, which) -> confirmar())
                .show();
    }

    private void confirmar() {
        if (remote != null) {
            binding.btnConfirmar.setEnabled(false);
            try {
                remote.checkout(reserva.id, binding.switchDanos.isChecked() ? montoDano() : 0,
                        texto(binding.etMotivoDano), texto(binding.etObservacionDano), lineasServicios())
                    .addOnSuccessListener(done -> { HotelAdminNotifications.cancelCheckout(this, reserva.id); Toast.makeText(this, "Cobro simulado registrado. Sin cargo bancario.", Toast.LENGTH_LONG).show(); finish(); })
                    .addOnFailureListener(error -> { binding.btnConfirmar.setEnabled(true); Toast.makeText(this, "No se pudo registrar. Revisa conexión, permisos y estado de la reserva.", Toast.LENGTH_LONG).show(); });
            } catch (RuntimeException error) { binding.btnConfirmar.setEnabled(true); Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show(); }
            return;
        }
        int consumosAntes = reserva.consumos.size();
        ReservaAdmin.EstadoCheckout estadoAntes = reserva.estado;
        if (binding.switchDanos.isChecked()) {
            reserva.consumos.add(new ItemConsumo(
                    getString(R.string.hoteladmin_cobro_dano_concepto), montoDano(),
                    texto(binding.etMotivoDano), texto(binding.etObservacionDano)));
        }
        reserva.estado = ReservaAdmin.EstadoCheckout.FINALIZADA;
        binding.btnConfirmar.setEnabled(false);
        HotelAdminLocalStore.get(this).saveCheckout(reserva, error -> {
            if (error != null) {
                reserva.estado = estadoAntes;
                while (reserva.consumos.size() > consumosAntes) {
                    reserva.consumos.remove(reserva.consumos.size() - 1);
                }
                binding.btnConfirmar.setEnabled(true);
                Toast.makeText(this, R.string.hoteladmin_error_guardado_local,
                        Toast.LENGTH_LONG).show();
                return;
            }
            Toast.makeText(this, R.string.hoteladmin_checkout_confirmado, Toast.LENGTH_LONG).show();
            HotelAdminNotifications.cancelCheckout(this, reserva.id);
            finish();
        });
    }

    private double montoDano() {
        try { return Double.parseDouble(texto(binding.etMontoDano)); }
        catch (NumberFormatException ignored) { return 0; }
    }

    private String texto(android.widget.EditText editText) {
        return String.valueOf(editText.getText()).trim();
    }

    private String moneda(double monto) {
        return getString(R.string.hoteladmin_moneda,
                String.format(Locale.US, "%,.2f", monto));
    }

    private static class SimpleTextWatcher implements TextWatcher {
        private final Runnable after;
        SimpleTextWatcher(Runnable after) { this.after = after; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
        @Override public void afterTextChanged(Editable s) { after.run(); }
    }
}
