package com.iotelito.taxiweb.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/** Firestore REST (v1) con el token de Firebase del usuario. Solo campos simples. */
@Component
public class FirestoreRestLector implements FirestoreLector {

    private static final Logger LOG = LoggerFactory.getLogger(FirestoreRestLector.class);
    private static final Pattern RUTA = Pattern.compile("[A-Za-z]+/[A-Za-z0-9_-]{1,128}");

    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();
    private final ObjectMapper json;
    private final String base;

    public FirestoreRestLector(ObjectMapper json, @Value("${iotelito.firebase.project-id}") String proyecto) {
        this.json = json;
        this.base = "https://firestore.googleapis.com/v1/projects/" + proyecto + "/databases/(default)/documents/";
    }

    @Override
    public Optional<Map<String, Object>> documento(String ruta, String token) {
        if (!RUTA.matcher(ruta).matches()) {
            throw ApiException.validacion("Identificador inválido.");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(Duration.ofSeconds(6))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> respuesta;
        try {
            respuesta = http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException error) {
            LOG.warn("Firestore no respondió al leer {}", ruta.substring(0, ruta.indexOf('/')));
            throw ApiException.noDisponible();
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw ApiException.noDisponible();
        }

        return switch (respuesta.statusCode()) {
            case 200 -> Optional.of(campos(respuesta.body()));
            case 404 -> Optional.empty();
            case 401, 403 -> throw ApiException.prohibido("No tienes acceso a ese dato.");
            default -> {
                LOG.warn("Firestore respondió {} al leer {}", respuesta.statusCode(), ruta.substring(0, ruta.indexOf('/')));
                throw ApiException.noDisponible();
            }
        };
    }

    private Map<String, Object> campos(String cuerpo) {
        try {
            Map<String, Object> resultado = new HashMap<>();
            JsonNode campos = json.readTree(cuerpo).path("fields");
            campos.fields().forEachRemaining(campo -> resultado.put(campo.getKey(), valor(campo.getValue())));
            return resultado;
        } catch (IOException error) {
            throw ApiException.noDisponible();
        }
    }

    private static Object valor(JsonNode nodo) {
        if (nodo.has("stringValue")) return nodo.get("stringValue").asText();
        if (nodo.has("integerValue")) return Long.parseLong(nodo.get("integerValue").asText());
        if (nodo.has("doubleValue")) return nodo.get("doubleValue").asDouble();
        if (nodo.has("booleanValue")) return nodo.get("booleanValue").asBoolean();
        if (nodo.has("timestampValue")) return nodo.get("timestampValue").asText();
        return null;
    }
}
