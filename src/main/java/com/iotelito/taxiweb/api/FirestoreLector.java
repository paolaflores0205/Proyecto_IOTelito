package com.iotelito.taxiweb.api;

import java.util.Map;
import java.util.Optional;

/**
 * Lee un documento de Firestore con el token de quien llama, así se aplican las reglas
 * publicadas y la web no guarda ninguna credencial de Firebase.
 * Vacío si no existe; {@link ApiException} si las reglas lo niegan o Firestore no responde.
 */
public interface FirestoreLector {

    Optional<Map<String, Object>> documento(String ruta, String token);
}
