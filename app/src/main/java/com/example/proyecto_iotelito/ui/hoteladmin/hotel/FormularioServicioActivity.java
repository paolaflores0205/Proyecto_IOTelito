package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.os.Bundle;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.proyecto_iotelito.data.hoteladmin.FirebaseHotelAdminRepository;
import com.example.proyecto_iotelito.data.ClientLocalTasks;
import com.example.proyecto_iotelito.data.HotelImageLoader;
import com.example.proyecto_iotelito.data.HotelPhotoStore;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.data.hoteladmin.HotelAdminLocalStore;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminFormularioServicioBinding;
import com.example.proyecto_iotelito.model.hoteladmin.Servicio;

import java.util.List;
import java.util.Collections;
import java.util.Locale;
import java.util.function.Consumer;

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
    private String draftPhoto;
    private final ActivityResultLauncher<String> picker = registerForActivityResult(
            new ActivityResultContracts.GetContent(), this::importPhoto);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!com.example.proyecto_iotelito.ui.hoteladmin.HotelAdminAccessGuard.allow(this)) return;
        binding = ActivityHoteladminFormularioServicioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.ivBack.setOnClickListener(v -> finish());
        if (FirebaseHotelAdminRepository.remote(this)) remote = new FirebaseHotelAdminRepository(this);
        draftPhoto = savedInstanceState == null ? null : savedInstanceState.getString("servicePhoto");
        binding.boxFoto.setOnClickListener(v -> {
            if (remote == null) Toast.makeText(this, R.string.hoteladmin_proximamente, Toast.LENGTH_SHORT).show();
            else picker.launch("image/*");
        });
        servicioId = getIntent().getIntExtra(EXTRA_SERVICIO_ID, -1);
        if (servicioId != -1) {
            binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_editar_servicio);
            if (remote == null) precargar(HotelAdminSampleData.servicio(servicioId));
            else {
                binding.btnGuardar.setEnabled(false);
                binding.boxFoto.setEnabled(false);
                remote.loadService(servicioId, service -> {
                    if (isFinishing() || isDestroyed()) return;
                    remoteOriginal = service; precargar(service); binding.btnGuardar.setEnabled(true);
                    binding.boxFoto.setEnabled(true);
                    showPhoto();
                }, error -> { if (!isFinishing()) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); finish(); } });
            }
        } else {
            binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_nuevo_servicio);
        }

        binding.btnGuardar.setOnClickListener(v -> guardar());
        showPhoto();
    }

    private void precargar(Servicio service) {
        binding.etNombre.setText(service.nombre); binding.etDescripcion.setText(service.descripcion);
        binding.etPrecio.setText(String.format(Locale.US, "%.2f", service.precio));
        binding.swDisponible.setChecked(service.activo);
    }

    private void guardar() {
        String nombre = binding.etNombre.getText().toString().trim();
        String precioStr = binding.etPrecio.getText().toString().trim();

        // Cada error en rojo debajo de su campo; Firestore exige los mismos límites.
        java.util.Map<String, String> errores = com.example.proyecto_iotelito.data.hoteladmin.InventarioValidacion
                .servicio(nombre, precioStr, binding.etDescripcion.getText().toString());
        boolean valido = com.example.proyecto_iotelito.ui.common.ErrorCampo.aplicar(binding.etNombre, errores.get("nombre"));
        valido &= com.example.proyecto_iotelito.ui.common.ErrorCampo.aplicar(binding.etPrecio, errores.get("precio"));
        valido &= com.example.proyecto_iotelito.ui.common.ErrorCampo.aplicar(binding.etDescripcion, errores.get("descripcion"));
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
            binding.boxFoto.setEnabled(false);
            Consumer<String> completed = error -> {
                if (isFinishing() || isDestroyed()) return;
                binding.btnGuardar.setEnabled(true);
                binding.boxFoto.setEnabled(true);
                if (error != null) { Toast.makeText(this, error, Toast.LENGTH_LONG).show(); return; }
                Toast.makeText(this, R.string.hoteladmin_servicio_guardado, Toast.LENGTH_SHORT).show(); finish();
            };
            if (draftPhoto == null) remote.saveService(service, servicioId == -1, completed);
            else remote.saveServiceWithPhoto(service, servicioId == -1, draftPhoto, completed);
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

    private void importPhoto(Uri uri) {
        if (uri == null || remote == null) return;
        binding.boxFoto.setEnabled(false);
        binding.btnGuardar.setEnabled(false);
        Runnable ready = () -> {
            binding.boxFoto.setEnabled(true);
            binding.btnGuardar.setEnabled(true);
        };
        ClientLocalTasks.run(this, this, () -> HotelPhotoStore.importPhotos(this, Collections.singletonList(uri)).get(0),
                photo -> { draftPhoto = photo; showPhoto(); ready.run(); }, ready);
    }

    private void showPhoto() {
        String photo = draftPhoto != null ? draftPhoto : remoteOriginal == null ? null : remoteOriginal.fotoUrl;
        binding.ivFotoServicio.setVisibility(photo == null ? View.GONE : View.VISIBLE);
        HotelImageLoader.show(binding.ivFotoServicio, photo);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state);
        state.putString("servicePhoto", draftPhoto);
    }
}
