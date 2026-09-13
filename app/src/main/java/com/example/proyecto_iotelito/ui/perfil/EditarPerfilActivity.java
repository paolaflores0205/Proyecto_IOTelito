package com.example.proyecto_iotelito.ui.perfil;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.SampleData;
import com.example.proyecto_iotelito.model.UserProfile;

/**
 * Edición de datos personales. Como todavía no hay backend/NoSQL, los
 * cambios se guardan en la misma instancia de {@link UserProfile} en
 * memoria, por lo que se reflejan de inmediato en el resto de la app.
 */
public class EditarPerfilActivity extends AppCompatActivity {

    private EditText etNombre;
    private EditText etEmail;
    private EditText etTelefono;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editar_perfil);

        View subHeader = findViewById(R.id.sub_header);
        subHeader.findViewById(R.id.iv_back).setOnClickListener(v -> finish());
        ((TextView) subHeader.findViewById(R.id.tv_title)).setText(R.string.titulo_editar_perfil);

        UserProfile perfil = SampleData.PERFIL;
        ((TextView) findViewById(R.id.tv_avatar_inicial)).setText(perfil.initials());

        etNombre = findViewById(R.id.et_nombre);
        etEmail = findViewById(R.id.et_email);
        etTelefono = findViewById(R.id.et_telefono);
        etNombre.setText(perfil.name);
        etEmail.setText(perfil.email);
        etTelefono.setText(perfil.phone);

        findViewById(R.id.btn_guardar_cambios).setOnClickListener(v -> onGuardar());
    }

    private void onGuardar() {
        String nombre = etNombre.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();

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
        perfil.email = email;
        perfil.phone = telefono;

        Toast.makeText(this, R.string.toast_perfil_actualizado, Toast.LENGTH_SHORT).show();
        finish();
    }
}
