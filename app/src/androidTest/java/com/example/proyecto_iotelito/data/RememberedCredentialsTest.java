package com.example.proyecto_iotelito.data;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.UUID;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class RememberedCredentialsTest {
    private Context context;
    private Context app;
    private String prefsName;

    @Before
    public void preparar() {
        app = InstrumentationRegistry.getInstrumentation().getTargetContext();
        prefsName = "test_recordarme_" + UUID.randomUUID();
        context = new ContextWrapper(app) {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                return super.getSharedPreferences(prefsName, mode);
            }
        };
    }

    @After
    public void limpiar() {
        app.deleteSharedPreferences(prefsName);
    }

    @Test
    public void cifraLaContrasenaYPermiteRecuperarla() throws Exception {
        RememberedCredentialsRepository.guardar(context, " Cliente@iotelito.pe ", "Clave de prueba 123");
        String almacenado = context.getSharedPreferences("credenciales_recordadas", Context.MODE_PRIVATE)
                .getString("cliente@iotelito.pe", "");
        assertFalse(almacenado.contains("Clave de prueba 123"));
        assertEquals("Clave de prueba 123", RememberedCredentialsRepository.password(context, "cliente@iotelito.pe"));
        assertEquals("cliente@iotelito.pe", RememberedCredentialsRepository.correos(context).get(0));
        RememberedCredentialsRepository.guardar(context, "cliente@iotelito.pe", "Nueva clave");
        assertEquals("Nueva clave", RememberedCredentialsRepository.password(context, "cliente@iotelito.pe"));
        assertEquals(1, RememberedCredentialsRepository.correos(context).size());
    }

    @Test
    public void olvidarUnaCuentaNoBorraLasOtras() throws Exception {
        RememberedCredentialsRepository.guardar(context, "cliente@iotelito.pe", "Clave cliente");
        RememberedCredentialsRepository.guardar(context, "taxista@iotelito.pe", "Clave taxista");
        RememberedCredentialsRepository.olvidar(context, "cliente@iotelito.pe");
        assertNull(RememberedCredentialsRepository.password(context, "cliente@iotelito.pe"));
        assertEquals(1, RememberedCredentialsRepository.correos(context).size());
        assertEquals("Clave taxista", RememberedCredentialsRepository.password(context, "taxista@iotelito.pe"));
    }
}
