package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.data.hoteladmin.HotelAdminLocalStore;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminFormularioServicioBinding;
import com.example.proyecto_iotelito.model.hoteladmin.Servicio;

import java.util.List;
import java.util.Locale;

/**
 * Formulario para registrar o editar un servicio (adm-formulario-servicio).
 * Con {@link #EXTRA_SERVICIO_ID} entra en modo edición. La unidad de cobro no se
 * edita aquí: se conserva la existente al editar y por defecto "servicio" al crear.
 */
public class FormularioServicioActivity extends AppCompatActivity {

    public static final String EXTRA_SERVICIO_ID = "extra_servicio_id";

    private ActivityHoteladminFormularioServicioBinding binding;
    private int servicioId = -1;
    private FirebaseHotelAdminRepository remote;
    private Servicio remoteOriginal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!com.example.proyecto_iotelito.ui.hoteladmin.HotelAdminAccessGuard.allow(this)) return;
        binding = ActivityHoteladminFormularioServicioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.ivBack.setOnClickListener(v -> finish());
        binding.boxFoto.setOnClickListener(v ->
                Toast.makeText(this, R.string.hoteladmin_proximamente, Toast.LENGTH_SHORT).show());

        if (FirebaseHotelAdminRepository.remote(this)) remote = new FirebaseHotelAdminRepository(this);
        servicioId = getIntent().getIntExtra(EXTRA_SERVICIO_ID, -1);
        if (servicioId != -1) {
            binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_editar_servicio);
            if (remote == null) precargar(HotelAdminSampleData.servicio(servicioId));
            else {
                binding.btnGuardar.setEnabled(false);
                remote.loadService(servicioId, service -> {
                    if (isFinishing() || isDestroyed()) return;
                    remoteOriginal = service; precargar(service); binding.btnGuardar.setEnabled(true);
                }, error -> { if (!isFinishing()) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); finish(); } });
            }
        } else {
            binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_nuevo_servicio);
        }

        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void precargar(Servicio service) {
        binding.etNombre.setText(service.nombre); binding.etDescripcion.setText(service.descripcion);
        binding.etPrecio.setText(String.format(Locale.US, "%.2f", service.precio));
        binding.swDisponible.setChecked(service.activo);
    }

    private void guardar() {
        String nombre = binding.etNombre.getText().toString().trim();
        String precioStr = binding.etPrecio.getText().toString().trim();

        boolean valido = true;
        if (TextUtils.isEmpty(nombre)) {
            binding.etNombre.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (TextUtils.isEmpty(precioStr)) {
            binding.etPrecio.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (!valido) {
            return;
        }

        String descripcion = binding.etDescripcion.getText().toString().trim();
        double precio = parseDouble(precioStr);
        boolean activo = binding.swDisponible.isChecked();

        if (remote != null) {
            if (servicioId != -1 && remoteOriginal == null) return;
            Servicio service = new Servicio(servicioId == -1 ? FirebaseHotelAdminRepository.newId() : servicioId,
                    remote.hotelId, nombre, descripcion, precio, remoteOriginal == null ? "servicio" : remoteOriginal.unidad,
                    R.drawable.superadmin_hotel_sol, activo);
            binding.btnGuardar.setEnabled(false);
            remote.saveService(service, servicioId == -1, error -> {
                if (isFinishing() || isDestroyed()) return;
                binding.btnGuardar.setEnabled(true);
                if (error != null) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); return; }
                Toast.makeText(this, R.string.hoteladmin_servicio_guardado, Toast.LENGTH_SHORT).show(); finish();
            });
            return;
        }
        Servicio guardado;
        if (servicioId != -1) {
            Servicio actual = HotelAdminSampleData.servicio(servicioId);
            guardado = new Servicio(servicioId, actual.hotelId, nombre, descripcion,
                    precio, actual.unidad, actual.fotoRes, activo);
        } else {
            int nuevoId = HotelAdminSampleData.nextServicioId();
            guardado = new Servicio(nuevoId, HotelAdminSampleData.HOTEL_ID, nombre, descripcion,
                    precio, "servicio", R.drawable.superadmin_hotel_sol, activo);
        }
        binding.btnGuardar.setEnabled(false);
        HotelAdminLocalStore.get(this).saveServicio(guardado, error -> {
            if (error != null) {
                binding.btnGuardar.setEnabled(true);
                Toast.makeText(this, R.string.hoteladmin_error_guardado_local, Toast.LENGTH_LONG).show();
                return;
            }
            List<Servicio> servicios = HotelAdminSampleData.servicios();
            if (servicioId == -1) {
                servicios.add(guardado);
            } else {
                for (int i = 0; i < servicios.size(); i++) {
                    if (servicios.get(i).id == servicioId) {
                        servicios.set(i, guardado);
                        break;
                    }
                }
            }
            Toast.makeText(this, R.string.hoteladmin_servicio_guardado, Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}
