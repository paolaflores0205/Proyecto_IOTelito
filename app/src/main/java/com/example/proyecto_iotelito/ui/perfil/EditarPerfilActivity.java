package com.example.proyecto_iotelito.ui.perfil;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.AutoCompleteTextView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.MainActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.UserProfile;
import com.google.android.material.bottomnavigation.BottomNavigationView;

/**
 * Edición de datos personales del perfil del cliente.
 * Utiliza un Exposed Dropdown Menu de Material Design para la selección elegante del Tipo de Documento.
 */
public class EditarPerfilActivity extends AppCompatActivity {

    private final String[] opcionesTipoDoc = new String[]{"DNI", "Carnet de Extranjería", "Pasaporte"};

    private EditText etNombre;
    private AutoCompleteTextView actvTipoDoc;
    private EditText etNumDoc;
    private EditText etEmail;
    private EditText etTelefono;
    private EditText etDomicilio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        // Header back icon
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        UserProfile perfil = SampleData.PERFIL;
        ((TextView) findViewById(R.id.tv_avatar_inicial)).setText(perfil.initials());

        etNombre = findViewById(R.id.et_nombre);
        actvTipoDoc = findViewById(R.id.actv_tipo_doc);
        etNumDoc = findViewById(R.id.et_num_doc);
        etEmail = findViewById(R.id.et_email);
        etTelefono = findViewById(R.id.et_telefono);
        etDomicilio = findViewById(R.id.et_domicilio);

        // Configuración del desplegable Material Design (Exposed Dropdown Menu)
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.item_dropdown_opcion, opcionesTipoDoc);
        actvTipoDoc.setAdapter(adapter);
        actvTipoDoc.setText(perfil.tipoDocumento != null ? perfil.tipoDocumento : "DNI", false);

        etNombre.setText(perfil.name);
        etNumDoc.setText(perfil.numeroDocumento);
        etEmail.setText(perfil.email);
        etTelefono.setText(perfil.phone);
        etDomicilio.setText(perfil.domicilio);

        // Botón Cancelar -> regresa a la vista de perfil sin guardar
        findViewById(R.id.btn_cancelar).setOnClickListener(v -> finish());

        // Botón Guardar -> guarda cambios y regresa a la vista de perfil
        findViewById(R.id.btn_guardar_cambios).setOnClickListener(v -> onGuardar());

        // Configuración de la navegación inferior
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_perfil);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_perfil) {
                finish();
                return true;
            } else {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(MainActivity.EXTRA_OPEN_TAB, id);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(intent);
                finish();
                return true;
            }
        });
    }

    private void onGuardar() {
        String nombre = etNombre.getText().toString().trim();
        String tipoDoc = actvTipoDoc.getText().toString().trim();
        String numDoc = etNumDoc.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String domicilio = etDomicilio.getText().toString().trim();

        boolean valido = true;
        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (TextUtils.isEmpty(email)) {
            etEmail.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (TextUtils.isEmpty(telefono)) {
            etTelefono.setError(getString(R.string.error_campo_obligatorio));
            valido = false;
        }
        if (!valido) {
            return;
        }

        UserProfile perfil = SampleData.PERFIL;
        perfil.name = nombre;
        perfil.tipoDocumento = tipoDoc;
        perfil.numeroDocumento = numDoc;
        perfil.email = email;
        perfil.phone = telefono;
        perfil.domicilio = domicilio;

        Toast.makeText(this, R.string.toast_perfil_actualizado, Toast.LENGTH_SHORT).show();
        finish();
    }
}
