package com.example.proyecto_iotelito.data;

import androidx.annotation.Nullable;

import com.example.proyecto_iotelito.model.auth.AuthenticatedUser;
import com.example.proyecto_iotelito.model.auth.UserRole;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class LocalAuthRepository {

    private static final LocalAccount[] ACCOUNTS = {
            new LocalAccount("cliente@iotelito.pe", "Carlos", UserRole.CLIENT,
                    "34e422278ea745b5d87ba6592f0ea3fe32a2eb7593f5960ac72d7094fb121f3d"),
            new LocalAccount("admin@iotelito.pe", "Administrador del hotel", UserRole.HOTEL_ADMIN,
                    "3b612c75a7b5048a435fb6ec81e52ff92d6d795a8b5a9c17070f6a63c97a53b2"),
            new LocalAccount("taxista@iotelito.pe", "Taxista IoTelito", UserRole.TAXISTA,
                    "228e50faae3d565b36d14451b417e3308af945dcf733e887a08ed4062f66df70"),
            new LocalAccount("superadmin@iotelito.pe", "Superadmin", UserRole.SUPERADMIN,
                    "996424412eb088d9afa0f55e81c44553384a94925d13ea601ef17be153d43ecb")
    };

    private LocalAuthRepository() { }

    @Nullable
    public static AuthenticatedUser authenticate(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        String passwordHash = sha256(password);

        for (LocalAccount account : ACCOUNTS) {
            if (account.email.equals(normalizedEmail)
                    && account.passwordHash.equals(passwordHash)) {
                return new AuthenticatedUser(account.email, account.displayName, account.role);
            }
        }
        return null;
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(encoded.length * 2);
            for (byte item : encoded) {
                result.append(String.format(Locale.ROOT, "%02x", item & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible", exception);
        }
    }

    private static final class LocalAccount {
        final String email;
        final String displayName;
        final UserRole role;
        final String passwordHash;

        LocalAccount(String email, String displayName, UserRole role, String passwordHash) {
            this.email = email;
            this.displayName = displayName;
            this.role = role;
            this.passwordHash = passwordHash;
        }
    }
}
