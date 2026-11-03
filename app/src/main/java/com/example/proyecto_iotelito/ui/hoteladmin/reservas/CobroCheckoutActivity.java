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
            binding.btnConfirmar.setText("Registrar checkout");
            remote.load(getIntent().getIntExtra(EXTRA_RESERVA_ID, -1), row -> {
                if (isFinishing() || isDestroyed()) return;
                reserva = row; mostrarReserva(); binding.btnConfirmar.setEnabled(true);
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

    private void actualizarTotal() {
        if (reserva == null) return;
        double total = reserva.total();
        if (binding.switchDanos.isChecked()) total += montoDano();
        binding.tvTotal.setText(moneda(total));
    }

    private void validarYConfirmar() {
        if (binding.switchDanos.isChecked()) {
            if (montoDano() <= 0) {
                binding.etMontoDano.setError(getString(R.string.hoteladmin_error_monto_dano));
                binding.etMontoDano.requestFocus();
                return;
            }
            if (texto(binding.etMotivoDano).isEmpty()) {
                binding.etMotivoDano.setError(getString(R.string.hoteladmin_campo_obligatorio));
                binding.etMotivoDano.requestFocus();
                return;
            }
            if (texto(binding.etObservacionDano).isEmpty()) {
                binding.etObservacionDano.setError(getString(R.string.hoteladmin_campo_obligatorio));
                binding.etObservacionDano.requestFocus();
                return;
            }
        }
        if (remote != null) {
            try { com.example.proyecto_iotelito.data.hoteladmin.HotelOperationsCodec.total(reserva.total(), binding.switchDanos.isChecked() ? montoDano() : 0); }
            catch (IllegalArgumentException error) { binding.etMontoDano.setError(error.getMessage()); return; }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(remote != null ? "¿Registrar checkout?" : getString(R.string.hoteladmin_confirmar_checkout_titulo))
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
                        texto(binding.etMotivoDano), texto(binding.etObservacionDano))
                    .addOnSuccessListener(done -> { Toast.makeText(this, "Checkout registrado. Sin cargo bancario.", Toast.LENGTH_LONG).show(); finish(); })
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
