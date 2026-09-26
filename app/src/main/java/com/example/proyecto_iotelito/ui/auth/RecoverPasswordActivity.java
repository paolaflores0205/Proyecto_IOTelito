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

public class RecoverPasswordActivity extends AppCompatActivity {

    public static final String EXTRA_EMAIL = "extra_email";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recover_password);

        setupHeader();
        TextInputLayout emailLayout = findViewById(R.id.recover_email_layout);
        TextInputEditText emailEditText = findViewById(R.id.recover_email_edit_text);

        findViewById(R.id.btn_enviar_recuperacion).setOnClickListener(v -> {
            String email = emailEditText.getText() == null ? "" : emailEditText.getText().toString().trim();
            emailLayout.setError(null);
            if (TextUtils.isEmpty(email)) {
                emailLayout.setError(getString(R.string.login_error_required));
                return;
            }
            if (!LocalAuthRepository.exists(email)) {
                emailLayout.setError(getString(R.string.recover_error_unknown_email));
                return;
            }
            Intent intent = new Intent(this, RecoverPasswordSentActivity.class);
            intent.putExtra(EXTRA_EMAIL, email);
            startActivity(intent);
        });
    }

    private void setupHeader() {
        View header = findViewById(R.id.sub_header);
        ImageView ivBack = header.findViewById(R.id.iv_back);
        TextView tvTitle = header.findViewById(R.id.tv_title);
        tvTitle.setText(R.string.recover_title);
        ivBack.setOnClickListener(v -> finish());
    }
}
