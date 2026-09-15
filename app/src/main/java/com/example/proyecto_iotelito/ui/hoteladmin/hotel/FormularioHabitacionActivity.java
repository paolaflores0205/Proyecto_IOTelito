package com.example.proyecto_iotelito.ui.hoteladmin.hotel;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.HotelAdminSampleData;
import com.example.proyecto_iotelito.databinding.ActivityHoteladminFormularioHabitacionBinding;
import com.example.proyecto_iotelito.model.hoteladmin.Habitacion;

import java.util.List;
import java.util.Locale;

/**
 * Formulario para registrar o editar una habitación (adm-formulario-habitacion).
 * Si recibe {@link #EXTRA_HABITACION_ID} funciona en modo edición (precarga los
 * datos); si no, crea una nueva. Guarda en memoria en {@link HotelAdminSampleData}.
 */
public class FormularioHabitacionActivity extends AppCompatActivity {

    public static final String EXTRA_HABITACION_ID = "extra_habitacion_id";

    private ActivityHoteladminFormularioHabitacionBinding binding;
    private int habitacionId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHoteladminFormularioHabitacionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.hoteladmin_tipos_habitacion, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spTipo.setAdapter(adapter);

        binding.toolbar.ivBack.setOnClickListener(v -> finish());
        binding.boxFoto.setOnClickListener(v ->
                Toast.makeText(this, R.string.hoteladmin_proximamente, Toast.LENGTH_SHORT).show());

        habitacionId = getIntent().getIntExtra(EXTRA_HABITACION_ID, -1);
        if (habitacionId != -1) {
            binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_editar_habitacion);
            precargar(HotelAdminSampleData.habitacion(habitacionId), adapter);
        } else {
            binding.toolbar.tvTitle.setText(R.string.hoteladmin_titulo_nueva_habitacion);
        }

        binding.btnGuardar.setOnClickListener(v -> guardar());
    }

    private void precargar(Habitacion h, ArrayAdapter<CharSequence> adapter) {
        int pos = adapter.getPosition(h.tipo);
        if (pos >= 0) binding.spTipo.setSelection(pos);
        binding.etCodigo.setText(h.nombreCodigo);
        binding.etArea.setText(String.valueOf(h.areaM2));
        binding.etAdultos.setText(String.valueOf(h.adultos));
        binding.etNinos.setText(String.valueOf(h.ninos));
        binding.etPrecio.setText(String.format(Locale.US, "%.0f", h.precioNoche));
        binding.etDescripcion.setText(h.descripcion);
        binding.swDisponible.setChecked(h.disponible);
    }

    private void guardar() {
        String codigo = binding.etCodigo.getText().toString().trim();
        String areaStr = binding.etArea.getText().toString().trim();
        String precioStr = binding.etPrecio.getText().toString().trim();

        boolean valido = true;
        if (TextUtils.isEmpty(codigo)) {
            binding.etCodigo.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (TextUtils.isEmpty(areaStr)) {
            binding.etArea.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (TextUtils.isEmpty(precioStr)) {
            binding.etPrecio.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (!valido) {
            return;
        }

        String tipo = binding.spTipo.getSelectedItem().toString();
        int area = parseInt(areaStr, 0);
        int adultos = parseInt(binding.etAdultos.getText().toString().trim(), 1);
        int ninos = parseInt(binding.etNinos.getText().toString().trim(), 0);
        double precio = parseDouble(precioStr);
        String descripcion = binding.etDescripcion.getText().toString().trim();
        boolean disponible = binding.swDisponible.isChecked();

        List<Habitacion> habitaciones = HotelAdminSampleData.habitaciones();
        if (habitacionId != -1) {
            Habitacion actual = HotelAdminSampleData.habitacion(habitacionId);
            Habitacion editada = new Habitacion(habitacionId, actual.hotelId, tipo, codigo, area,
                    adultos, ninos, precio, descripcion, actual.fotoRes, disponible);
            for (int i = 0; i < habitaciones.size(); i++) {
                if (habitaciones.get(i).id == habitacionId) {
                    habitaciones.set(i, editada);
                    break;
                }
            }
        } else {
            int nuevoId = HotelAdminSampleData.nextHabitacionId();
            habitaciones.add(new Habitacion(nuevoId, HotelAdminSampleData.HOTEL_ID, tipo, codigo,
                    area, adultos, ninos, precio, descripcion, R.drawable.superadmin_hotel_sol, disponible));
        }

        Toast.makeText(this, R.string.hoteladmin_habitacion_guardada, Toast.LENGTH_SHORT).show();
        finish();
    }

    private int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
