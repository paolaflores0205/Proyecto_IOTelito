package com.example.proyecto_iotelito.ui.auth;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Patterns;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.proyecto_iotelito.R;
import com.example.proyecto_iotelito.data.PerfilRepository;
import com.example.proyecto_iotelito.model.UserProfile;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class RegisterActivity extends AppCompatActivity {

    private static final DateTimeFormatter BIRTHDATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private TextInputLayout namesLayout;
    private TextInputLayout lastnamesLayout;
    private TextInputLayout documentLayout;
    private TextInputLayout birthdateLayout;
    private TextInputLayout emailLayout;
    private TextInputLayout passwordLayout;
    private TextInputLayout phoneLayout;
    private TextInputLayout addressLayout;

    private TextInputEditText namesEditText;
    private TextInputEditText lastnamesEditText;
    private TextInputEditText documentEditText;
    private TextInputEditText birthdateEditText;
    private TextInputEditText emailEditText;
    private TextInputEditText passwordEditText;
    private TextInputEditText phoneEditText;
    private TextInputEditText addressEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        setupHeader();
        bindFields();
        setupBirthdateInput();
        findViewById(R.id.btn_registrarme).setOnClickListener(v -> attemptRegister());
    }

    private void attemptRegister() {
        clearErrors();

        String names = valueOf(namesEditText.getText());
        String lastnames = valueOf(lastnamesEditText.getText());
        String document = valueOf(documentEditText.getText());
        String birthdate = valueOf(birthdateEditText.getText());
        String email = valueOf(emailEditText.getText());
        String password = valueOf(passwordEditText.getText());
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
        }
        if (password.length() < 6) {
            passwordLayout.setError(getString(R.string.register_error_password));
            hasError = true;
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

        if (hasError) return;

        View submitButton = findViewById(R.id.btn_registrarme);
        submitButton.setEnabled(false);
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            submitButton.setEnabled(true);
            if (!task.isSuccessful()) {
                Exception error = task.getException();
                if (error instanceof FirebaseAuthUserCollisionException) {
                    emailLayout.setError(getString(R.string.register_error_email_exists));
                } else {
                    Toast.makeText(this, error instanceof FirebaseNetworkException
                            ? R.string.register_error_network : R.string.register_error_generic,
                            Toast.LENGTH_LONG).show();
                }
                return;
            }

            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, R.string.register_error_generic, Toast.LENGTH_LONG).show();
                return;
            }
            String fullName = names + " " + lastnames;
            PerfilRepository.guardar(this, new UserProfile(fullName, email, phone,
                    "DNI", document, birthdate, address));
            user.updateProfile(new UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName).build()).addOnCompleteListener(this, profileTask -> {
                auth.signOut();
                Toast.makeText(this, R.string.register_success_demo, Toast.LENGTH_LONG).show();
                finish();
            });
        });
    }

    private void bindFields() {
        namesLayout = findViewById(R.id.register_names_layout);
        lastnamesLayout = findViewById(R.id.register_lastnames_layout);
        documentLayout = findViewById(R.id.register_document_layout);
        birthdateLayout = findViewById(R.id.register_birthdate_layout);
        emailLayout = findViewById(R.id.register_email_layout);
        passwordLayout = findViewById(R.id.register_password_layout);
        phoneLayout = findViewById(R.id.register_phone_layout);
        addressLayout = findViewById(R.id.register_address_layout);

        namesEditText = findViewById(R.id.register_names_edit_text);
        lastnamesEditText = findViewById(R.id.register_lastnames_edit_text);
        documentEditText = findViewById(R.id.register_document_edit_text);
        birthdateEditText = findViewById(R.id.register_birthdate_edit_text);
        emailEditText = findViewById(R.id.register_email_edit_text);
        passwordEditText = findViewById(R.id.register_password_edit_text);
        phoneEditText = findViewById(R.id.register_phone_edit_text);
        addressEditText = findViewById(R.id.register_address_edit_text);
    }

    private void clearErrors() {
        namesLayout.setError(null);
        lastnamesLayout.setError(null);
        documentLayout.setError(null);
        birthdateLayout.setError(null);
        emailLayout.setError(null);
        passwordLayout.setError(null);
        phoneLayout.setError(null);
        addressLayout.setError(null);
    }

    private void setupBirthdateInput() {
        birthdateEditText.setKeyListener(DigitsKeyListener.getInstance("0123456789/"));
        birthdateEditText.addTextChangedListener(new TextWatcher() {
            private boolean formatting;
            private int digitToDelete = -1;

            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {
                digitToDelete = -1;
                if (count == 1 && after == 0 && text.charAt(start) == '/') {
                    int digitsBefore = 0;
                    for (int i = 0; i < start; i++) {
                        if (Character.isDigit(text.charAt(i))) digitsBefore++;
                    }
                    digitToDelete = digitsBefore - 1;
                }
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable text) {
                if (formatting) return;
                int selection = Math.max(0, birthdateEditText.getSelectionStart());
                int digitsBeforeCursor = 0;
                for (int i = 0; i < Math.min(selection, text.length()); i++) {
                    if (Character.isDigit(text.charAt(i))) digitsBeforeCursor++;
                }
                String digits = text.toString().replaceAll("\\D", "");
                if (digitToDelete >= 0 && digitToDelete < digits.length()) {
                    digits = digits.substring(0, digitToDelete) + digits.substring(digitToDelete + 1);
                    digitsBeforeCursor = Math.max(0, digitsBeforeCursor - 1);
                }
                if (digits.length() > 8) digits = digits.substring(0, 8);

                StringBuilder formatted = new StringBuilder(10);
                for (int i = 0; i < digits.length(); i++) {
                    formatted.append(digits.charAt(i));
                    if (i == 1 || i == 3) formatted.append('/');
                }
                if (!formatted.toString().contentEquals(text)) {
                    formatting = true;
                    text.replace(0, text.length(), formatted);
                    int cursor = 0;
                    int seenDigits = 0;
                    while (cursor < formatted.length() && seenDigits < digitsBeforeCursor) {
                        if (Character.isDigit(formatted.charAt(cursor))) seenDigits++;
                        cursor++;
                    }
                    while (cursor < formatted.length() && formatted.charAt(cursor) == '/') cursor++;
                    birthdateEditText.setSelection(cursor);
                    formatting = false;
                }
                digitToDelete = -1;
            }
        });
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
