package com.example.proyecto_iotelito.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;

public class RecoverPasswordSentActivity extends AppCompatActivity {

    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recover_password_sent);

        email = getIntent().getStringExtra(RecoverPasswordActivity.EXTRA_EMAIL);
        if (email == null || email.trim().isEmpty()) {
            email = getString(R.string.login_email_hint);
        }
        ((TextView) findViewById(R.id.tv_recover_sent_email)).setText(
                getString(R.string.recover_sent_email, email));

        findViewById(R.id.btn_crear_nueva_contrasena).setOnClickListener(v -> {
            Intent intent = new Intent(this, NewPasswordActivity.class);
            intent.putExtra(RecoverPasswordActivity.EXTRA_EMAIL, email);
            startActivity(intent);
        });
        findViewById(R.id.btn_volver_login).setOnClickListener(v -> finish());
    }
}
