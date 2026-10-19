package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class RememberedCredentialsRepository {
    private static final String KEY_ALIAS = "iotelito_recordarme_v1";

    private RememberedCredentialsRepository() { }

    private static SharedPreferences preferencias(Context context) {
        return context.getSharedPreferences("credenciales_recordadas", Context.MODE_PRIVATE);
    }

    private static String normalizar(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public static List<String> correos(Context context) {
        List<String> emails = new ArrayList<>(preferencias(context).getAll().keySet());
        Collections.sort(emails);
        return emails;
    }

    public static void guardar(Context context, String email, String password)
            throws GeneralSecurityException, IOException {
        String cuenta = normalizar(email);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, clave());
        cipher.updateAAD(cuenta.getBytes(StandardCharsets.UTF_8));
        byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        String value = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":"
                + Base64.encodeToString(encrypted, Base64.NO_WRAP);
        preferencias(context).edit().putString(cuenta, value).apply();
    }

    public static String password(Context context, String email)
            throws GeneralSecurityException, IOException {
        String cuenta = normalizar(email);
        String value = preferencias(context).getString(cuenta, null);
        if (value == null) return null;
        try {
            String[] parts = value.split(":", -1);
            if (parts.length != 2) throw new GeneralSecurityException("Credencial inválida");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, clave(),
                    new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
            cipher.updateAAD(cuenta.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            throw new GeneralSecurityException("Credencial inválida", exception);
        }
    }

    public static void olvidar(Context context, String email) {
        preferencias(context).edit().remove(normalizar(email)).apply();
    }

    private static synchronized SecretKey clave() throws GeneralSecurityException, IOException {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) return (SecretKey) store.getKey(KEY_ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
}
