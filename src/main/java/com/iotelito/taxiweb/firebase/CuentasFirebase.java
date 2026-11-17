package com.iotelito.taxiweb.firebase;

import java.util.Optional;

/**
 * Cuentas de la app para los taxistas: se crean al aprobarlos en el panel y se bloquean al
 * rechazarlos o desactivarlos. Así nadie tiene que entrar a la consola de Firebase.
 */
public interface CuentasFirebase {

    /**
     * Deja lista la cuenta del taxista: la crea si no existe (y Firebase le envía un correo para
     * elegir su contraseña), la reactiva si estaba bloqueada y escribe users/{uid} con rol TAXISTA.
     *
     * @return el uid de Firebase, o vacío si la automatización no está configurada
     * @throws CuentaFirebaseException con un mensaje para el superadmin si no se pudo
     */
    Optional<String> prepararCuentaTaxista(DatosTaxista taxista);

    /** Bloquea la cuenta y marca users/{uid}.active = false. No hace nada si no está configurada. */
    void bloquear(String uid);

    record DatosTaxista(String uidActual, String correo, String nombres, String apellidos, String telefono) {
    }
}
