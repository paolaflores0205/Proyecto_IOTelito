package com.example.proyecto_iotelito.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.LocalAuthRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class NewPasswordActivity extends AppCompatActivity {

    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_password);

        email = getIntent().getStringExtra(RecoverPasswordActivity.EXTRA_EMAIL);
        setupHeader();
        TextInputLayout passwordLayout = findViewById(R.id.new_password_layout);
        TextInputLayout confirmLayout = findViewById(R.id.confirm_password_layout);
        TextInputEditText passwordEditText = findViewById(R.id.new_password_edit_text);
        TextInputEditText confirmEditText = findViewById(R.id.confirm_password_edit_text);

        findViewById(R.id.btn_cambiar_contrasena).setOnClickListener(v -> {
            String password = valueOf(passwordEditText.getText());
            String confirm = valueOf(confirmEditText.getText());
            passwordLayout.setError(null);
            confirmLayout.setError(null);

            if (TextUtils.isEmpty(password)) {
                passwordLayout.setError(getString(R.string.login_error_required));
                return;
            }
            if (!password.equals(confirm)) {
                confirmLayout.setError(getString(R.string.new_password_error_match));
                return;
            }
            if (email == null || !LocalAuthRepository.updatePassword(this, email, password)) {
                passwordLayout.setError(getString(R.string.recover_error_unknown_email));
                return;
            }
            startActivity(new Intent(this, PasswordUpdatedActivity.class));
        });
    }

    private void setupHeader() {
        View header = findViewById(R.id.sub_header);
        ImageView ivBack = header.findViewById(R.id.iv_back);
        TextView tvTitle = header.findViewById(R.id.tv_title);
        tvTitle.setText(R.string.new_password_title);
        ivBack.setOnClickListener(v -> finish());
    }

    private String valueOf(CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
