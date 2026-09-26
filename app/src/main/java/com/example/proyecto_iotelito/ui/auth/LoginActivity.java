package com.example.proyecto_iotelito.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.MainActivity;
import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.LocalAuthRepository;
import com.example.proyecto_iotelito.databinding.ActivityLoginBinding;
import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.ui.hoteladmin.HotelAdminMainActivity;
import com.example.proyecto_iotelito.ui.superadmin.SuperadminMainActivity;
import com.google.android.material.snackbar.Snackbar;
import com.example.proyecto_iotelito.ui.taxista.TaxistaMainActivity;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

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

    @Override
    protected void onResume() {
        super.onResume();
        if (binding != null) {
            binding.passwordEditText.setText("");
        }
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

        Intent destination;
        switch (user.getRole()) {
            case CLIENT:
                destination = new Intent(this, MainActivity.class);
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
    }

    private String valueOf(@Nullable CharSequence value) {
        return value == null ? "" : value.toString();
    }

}
