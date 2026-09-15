package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHoteladminFormularioServicioBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.toolbar.ivBack.setOnClickListener(v -> finish());
        binding.boxFoto.setOnClickListener(v ->
                Toast.makeText(this, R.string.hoteladmin_proximamente, Toast.LENGTH_SHORT).show());

        servicioId = getIntent().getIntExtra(EXTRA_SERVICIO_ID, -1);
        if (servicioId != -1) {
            binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_editar_servicio);
            Servicio s = HotelAdminSampleData.servicio(servicioId);
            binding.etNombre.setText(s.nombre);
            binding.etDescripcion.setText(s.descripcion);
            binding.etPrecio.setText(String.format(Locale.US, "%.0f", s.precio));
            binding.swDisponible.setChecked(s.activo);
        } else {
            binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_nuevo_servicio);
        }

        binding.btnGuardar.setOnClickListener(v -> guardar());
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

        List<Servicio> servicios = HotelAdminSampleData.servicios();
        if (servicioId != -1) {
            Servicio actual = HotelAdminSampleData.servicio(servicioId);
            Servicio editado = new Servicio(servicioId, actual.hotelId, nombre, descripcion,
                    precio, actual.unidad, actual.fotoRes, activo);
            for (int i = 0; i < servicios.size(); i++) {
                if (servicios.get(i).id == servicioId) {
                    servicios.set(i, editado);
                    break;
                }
            }
        } else {
            int nuevoId = HotelAdminSampleData.nextServicioId();
            servicios.add(new Servicio(nuevoId, HotelAdminSampleData.HOTEL_ID, nombre, descripcion,
                    precio, "servicio", R.drawable.superadmin_hotel_sol, activo));
        }

        Toast.makeText(this, R.string.hoteladmin_servicio_guardado, Toast.LENGTH_SHORT).show();
        finish();
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
