package com.iotelito.taxiweb.firebase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.GoogleCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Firebase por REST con la cuenta de servicio (Identity Toolkit para Auth y Firestore REST),
 * sin el SDK completo para que el servidor siga liviano. El correo para elegir contraseña lo
 * envía Firebase con la clave web pública del proyecto (la misma que trae la app).
 */
public class CuentasFirebaseRest implements CuentasFirebase {

    private static final Logger LOG = LoggerFactory.getLogger(CuentasFirebaseRest.class);
    private static final String AUTH = "https://identitytoolkit.googleapis.com/v1/";

    private final GoogleCredentials credenciales;
    private final String proyecto;
    private final String claveWeb;
    private final ObjectMapper json;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public CuentasFirebaseRest(InputStream cuentaServicio, String proyecto, String claveWeb, ObjectMapper json)
            throws IOException {
        this.credenciales = GoogleCredentials.fromStream(cuentaServicio)
                .createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        this.proyecto = proyecto;
        this.claveWeb = claveWeb;
        this.json = json;
    }

    @Override
    public Optional<String> prepararCuentaTaxista(DatosTaxista taxista) {
        String uid = existente(taxista.uidActual()).orElse(null);
        boolean nueva = false;
        if (uid == null) {
            uid = porCorreo(taxista.correo()).orElse(null);
        }
        if (uid == null) {
            ObjectNode cuerpo = json.createObjectNode()
                    .put("email", taxista.correo())
                    .put("emailVerified", false)
                    .put("disabled", false)
                    .put("displayName", taxista.nombres() + " " + taxista.apellidos());
            uid = llamar("POST", AUTH + "projects/" + proyecto + "/accounts", cuerpo, true).path("localId").asText(null);
            if (uid == null) {
                throw new CuentaFirebaseException("Firebase no devolvió la cuenta creada.");
            }
            nueva = true;
        } else {
            String rol = rolEnFirestore(uid);
            if (rol != null && !"TAXISTA".equals(rol)) {
                throw new CuentaFirebaseException("El correo " + taxista.correo()
                        + " ya tiene una cuenta de " + rol + " en la app. Pide al taxista que se registre con otro correo.");
            }
            llamar("POST", AUTH + "projects/" + proyecto + "/accounts:update",
                    json.createObjectNode().put("localId", uid).put("disableUser", false), true);
        }

        escribirPerfil(uid, taxista);
        if (nueva) {
            enviarCorreoContrasena(taxista.correo());
        }
        LOG.info("Cuenta de Firebase del taxista lista ({})", nueva ? "creada" : "reactivada");
        return Optional.of(uid);
    }

    @Override
    public void bloquear(String uid) {
        if (uid == null || existente(uid).isEmpty()) {
            return;
        }
        llamar("POST", AUTH + "projects/" + proyecto + "/accounts:update",
                json.createObjectNode().put("localId", uid).put("disableUser", true), true);
        ObjectNode campos = json.createObjectNode();
        campos.putObject("active").put("booleanValue", false);
        campos.putObject("statusReason").put("stringValue", "Desactivado desde la web de taxistas");
        campos.putObject("updatedAt").put("timestampValue", Instant.now().toString());
        actualizarUsuario(uid, campos);
    }

    // ------------------------------------------------------------------ Auth

    private Optional<String> existente(String uid) {
        if (uid == null || uid.isBlank()) {
            return Optional.empty();
        }
        JsonNode usuarios = llamar("POST", AUTH + "projects/" + proyecto + "/accounts:lookup",
                json.createObjectNode().set("localId", json.createArrayNode().add(uid)), true).path("users");
        return usuarios.isArray() && !usuarios.isEmpty() ? Optional.of(uid) : Optional.empty();
    }

    private Optional<String> porCorreo(String correo) {
        JsonNode usuarios = llamar("POST", AUTH + "projects/" + proyecto + "/accounts:lookup",
                json.createObjectNode().set("email", json.createArrayNode().add(correo)), true).path("users");
        return usuarios.isArray() && !usuarios.isEmpty()
                ? Optional.ofNullable(usuarios.get(0).path("localId").asText(null))
                : Optional.empty();
    }

    private void enviarCorreoContrasena(String correo) {
        if (claveWeb == null || claveWeb.isBlank()) {
            LOG.warn("Sin clave web de Firebase: no se envió el correo para elegir contraseña");
            return;
        }
        llamar("POST", AUTH + "accounts:sendOobCode?key=" + URLEncoder.encode(claveWeb, StandardCharsets.UTF_8),
                json.createObjectNode().put("requestType", "PASSWORD_RESET").put("email", correo), false);
    }

    // ------------------------------------------------------------------ Firestore

    private String documento(String uid) {
        return "https://firestore.googleapis.com/v1/projects/" + proyecto + "/databases/(default)/documents/users/"
                + URLEncoder.encode(uid, StandardCharsets.UTF_8);
    }

    private String rolEnFirestore(String uid) {
        try {
            return llamar("GET", documento(uid), null, true).path("fields").path("role").path("stringValue").asText(null);
        } catch (NoEncontrado error) {
            return null;
        }
    }

    private void escribirPerfil(String uid, DatosTaxista taxista) {
        ObjectNode campos = json.createObjectNode();
        campos.putObject("uid").put("stringValue", uid);
        campos.putObject("email").put("stringValue", taxista.correo());
        campos.putObject("name").put("stringValue", taxista.nombres() + " " + taxista.apellidos());
        campos.putObject("firstNames").put("stringValue", taxista.nombres());
        campos.putObject("lastNames").put("stringValue", taxista.apellidos());
        campos.putObject("phone").put("stringValue", taxista.telefono() == null ? "" : taxista.telefono());
        campos.putObject("role").put("stringValue", "TAXISTA");
        campos.putObject("active").put("booleanValue", true);
        campos.putObject("updatedAt").put("timestampValue", Instant.now().toString());
        actualizarUsuario(uid, campos);
    }

    /** PATCH con updateMask: crea el documento si no existe y no borra los demás campos. */
    private void actualizarUsuario(String uid, ObjectNode campos) {
        StringBuilder url = new StringBuilder(documento(uid)).append('?');
        campos.fieldNames().forEachRemaining(campo -> url.append("updateMask.fieldPaths=").append(campo).append('&'));
        ObjectNode cuerpo = json.createObjectNode();
        cuerpo.set("fields", campos);
        llamar("PATCH", url.substring(0, url.length() - 1), cuerpo, true);
    }

    // ------------------------------------------------------------------ HTTP

    private static final class NoEncontrado extends RuntimeException {
    }

    private JsonNode llamar(String metodo, String url, JsonNode cuerpo, boolean conCuentaServicio) {
        try {
            HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(cuerpo)));
            if (conCuentaServicio) {
                credenciales.refreshIfExpired();
                peticion.header("Authorization", "Bearer " + credenciales.getAccessToken().getTokenValue());
            }
            HttpResponse<String> respuesta = http.send(peticion.build(), HttpResponse.BodyHandlers.ofString());
            if (respuesta.statusCode() == 404) {
                throw new NoEncontrado();
            }
            if (respuesta.statusCode() / 100 != 2) {
                String detalle = json.readTree(respuesta.body()).path("error").path("message").asText("");
                LOG.warn("Firebase respondió {} ({})", respuesta.statusCode(), detalle);
                throw new CuentaFirebaseException("Firebase rechazó la operación (" + detalle + ").");
            }
            return respuesta.body().isBlank() ? json.createObjectNode() : json.readTree(respuesta.body());
        } catch (IOException error) {
            LOG.warn("No se pudo conectar con Firebase: {}", error.getMessage());
            throw new CuentaFirebaseException("No se pudo conectar con Firebase. Intenta de nuevo en unos minutos.");
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new CuentaFirebaseException("Operación interrumpida.");
        }
    }
}
