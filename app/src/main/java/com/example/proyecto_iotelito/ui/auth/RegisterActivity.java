package com.example.proyecto_iotelito.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.LocalAuthRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class RegisterActivity extends AppCompatActivity {

    private static final DateTimeFormatter BIRTHDATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private TextInputLayout namesLayout;
    private TextInputLayout lastnamesLayout;
    private TextInputLayout documentLayout;
    private TextInputLayout birthdateLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout addressLayout;

    private TextInputEditText namesEditText;
    private TextInputEditText lastnamesEditText;
    private TextInputEditText documentEditText;
    private TextInputEditText birthdateEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText phoneEditText;
    private TextInputEditText addressEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        setupHeader();
        bindFields();
        findViewById(R.id.btn_registrarme).setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        clearErrors();

        String names = valueOf(namesEditText.getText());
        String lastnames = valueOf(lastnamesEditText.getText());
        String document = valueOf(documentEditText.getText());
        String birthdate = valueOf(birthdateEditText.getText());
        String email = valueOf(emailEditText.getText());
        String phone = valueOf(phoneEditText.getText()).replace(" ", "");
        String address = valueOf(addressEditText.getText());
        boolean hasError = false;

        if (names.isEmpty()) {
            namesLayout.setError(getString(R.string.register_error_name));
            hasError = true;
        }
        if (lastnames.isEmpty()) {
            lastnamesLayout.setError(getString(R.string.register_error_lastname));
            hasError = true;
        }
        if (!document.matches("\\d{8}")) {
            documentLayout.setError(getString(R.string.register_error_document));
            hasError = true;
        }
        if (!isAdultBirthdate(birthdate)) {
            birthdateLayout.setError(getString(R.string.register_error_birthdate));
            hasError = true;
        }
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.setError(getString(R.string.register_error_email));
            hasError = true;
        } else if (LocalAuthRepository.exists(email)) {
            emailLayout.setError(getString(R.string.register_error_email_exists));
            hasError = true;
        }
        if (!phone.matches("\\d{9}")) {
            phoneLayout.setError(getString(R.string.register_error_phone));
            hasError = true;
        }
        if (address.length() < 8) {
            addressLayout.setError(getString(R.string.register_error_address));
            hasError = true;
        }

        if (!hasError) {
            Toast.makeText(this, R.string.register_success_demo, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void bindFields() {
        namesLayout = findViewById(R.id.register_names_layout);
        lastnamesLayout = findViewById(R.id.register_lastnames_layout);
        documentLayout = findViewById(R.id.register_document_layout);
        birthdateLayout = findViewById(R.id.register_birthdate_layout);
        emailLayout = findViewById(R.id.register_email_layout);
        phoneLayout = findViewById(R.id.register_phone_layout);
        addressLayout = findViewById(R.id.register_address_layout);

        namesEditText = findViewById(R.id.register_names_edit_text);
        lastnamesEditText = findViewById(R.id.register_lastnames_edit_text);
        documentEditText = findViewById(R.id.register_document_edit_text);
        birthdateEditText = findViewById(R.id.register_birthdate_edit_text);
        emailEditText = findViewById(R.id.register_email_edit_text);
        phoneEditText = findViewById(R.id.register_phone_edit_text);
        addressEditText = findViewById(R.id.register_address_edit_text);
    }

    private void clearErrors() {
        namesLayout.setError(null);
        lastnamesLayout.setError(null);
        documentLayout.setError(null);
        birthdateLayout.setError(null);
        emailLayout.setError(null);
        phoneLayout.setError(null);
        addressLayout.setError(null);
    }

    private boolean isAdultBirthdate(String value) {
        try {
            LocalDate birthdate = LocalDate.parse(value, BIRTHDATE_FORMATTER);
            return !birthdate.isAfter(LocalDate.now())
                    && Period.between(birthdate, LocalDate.now()).getYears() >= 18;
        } catch (DateTimeParseException exception) {
            return false;
        }
    }

    private String valueOf(CharSequence value) {
        return value == null ? "" : value.toString().trim();
    }

    private void setupHeader() {
        View header = findViewById(R.id.sub_header);
        ImageView ivBack = header.findViewById(R.id.iv_back);
        TextView tvTitle = header.findViewById(R.id.tv_title);
        tvTitle.setText(R.string.register_title);
        ivBack.setOnClickListener(v -> finish());
    }
}
