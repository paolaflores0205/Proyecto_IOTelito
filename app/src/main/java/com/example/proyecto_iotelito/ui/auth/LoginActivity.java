package com.example.proyecto_iotelito.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.MainActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.LocalAuthRepository;
import com.example.proyecto_iotelito.data.RememberedCredentialsRepository;
import com.example.proyecto_iotelito.data.SessionRepository;
import com.example.proyecto_iotelito.databinding.ActivityLoginBinding;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.ui.hoteladmin.HotelAdminMainActivity;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminMainActivity;
import java.io.IOException;
import java.security.GeneralSecurityException;
import com.example.proyecto_iotelito.ui.taxista.TaxistaMainActivity;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        configurarRecordarme();

        binding.loginButton.setOnClickListener(view -> attemptLogin());
        binding.passwordEditText.setOnEditorActionListener((view, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                attemptLogin();
                return true;
            }
            return false;
        });
        binding.forgotPasswordButton.setOnClickListener(view ->
                startActivity(new Intent(this, RecoverPasswordActivity.class)));
        binding.createAccountButton.setOnClickListener(view ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void configurarRecordarme() {
        actualizarCorreos();
        binding.rememberMe.setChecked(!RememberedCredentialsRepository.correos(this).isEmpty());
        binding.emailEditText.setOnClickListener(view -> binding.emailEditText.showDropDown());
        binding.emailEditText.setOnFocusChangeListener((view, focused) -> {
            if (focused) binding.emailEditText.post(() -> {
                if (binding.emailEditText.hasFocus()) binding.emailEditText.showDropDown();
            });
        });
        binding.emailEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) {
                binding.passwordEditText.setText("");
            }
            @Override public void afterTextChanged(Editable text) { }
        });
        binding.emailEditText.setOnItemClickListener((parent, view, position, id) -> {
            String email = parent.getItemAtPosition(position).toString();
            try {
                binding.passwordEditText.setText(RememberedCredentialsRepository.password(this, email));
                binding.rememberMe.setChecked(true);
            } catch (GeneralSecurityException | IOException exception) {
                RememberedCredentialsRepository.olvidar(this, email);
                actualizarCorreos();
                Toast.makeText(this, R.string.login_autocompletar_error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void actualizarCorreos() {
        binding.emailEditText.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, RememberedCredentialsRepository.correos(this)));
    }

    private void attemptLogin() {
        binding.emailLayout.setError(null);
        binding.passwordLayout.setError(null);

        String email = valueOf(binding.emailEditText.getText());
        String password = valueOf(binding.passwordEditText.getText());
        boolean hasError = false;

        if (TextUtils.isEmpty(email)) {
            binding.emailLayout.setError(getString(R.string.login_error_required));
            hasError = true;
        }
        if (TextUtils.isEmpty(password)) {
            binding.passwordLayout.setError(getString(R.string.login_error_required));
            hasError = true;
        }
        if (hasError) return;

        AuthenticatedUser user = LocalAuthRepository.authenticate(this, email, password);
        if (user == null) {
            binding.passwordLayout.setError(getString(R.string.login_error_credentials));
            return;
        }

        if (binding.rememberMe.isChecked()) {
            try {
                RememberedCredentialsRepository.guardar(this, user.getEmail(), password);
            } catch (GeneralSecurityException | IOException exception) {
                Toast.makeText(this, R.string.login_recordar_error, Toast.LENGTH_LONG).show();
                return;
            }
        } else {
            RememberedCredentialsRepository.olvidar(this, user.getEmail());
        }
        SessionRepository.guardar(this, user);
        Intent destination;
        switch (user.getRole()) {
            case CLIENT:
                destination = clienteDestination();
                break;
            case SUPERADMIN:
                destination = new Intent(this, SuperadminMainActivity.class);
                break;
            case HOTEL_ADMIN:
                destination = new Intent(this, HotelAdminMainActivity.class);
                break;
            case TAXISTA:
                destination = new Intent(this, TaxistaMainActivity.class);
                break;
            default:
                throw new IllegalStateException("Rol no soportado: " + user.getRole());
        }
        startActivity(destination);
        finish();
    }

    private Intent clienteDestination() {
        int id = getIntent().getIntExtra(com.example.proyecto_iotelito.data.TaxiRepository.EXTRA_ID, -1);
        return id < 0 ? new Intent(this, MainActivity.class)
                : new Intent(this, com.example.proyecto_iotelito.ui.taxi.TaxistaAsignadoActivity.class)
                .putExtra(com.example.proyecto_iotelito.data.TaxiRepository.EXTRA_ID, id);
    }

    private String valueOf(@Nullable CharSequence value) {
        return value == null ? "" : value.toString();
    }

}
