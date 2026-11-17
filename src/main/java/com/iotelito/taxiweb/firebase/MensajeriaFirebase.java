package com.iotelito.taxiweb.firebase;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MensajeriaFirebase {
    public enum Resultado { ENVIADO, TOKEN_REVOCADO, REINTENTAR }

    private final ObjectMapper json;
    private final GoogleCredentials cuenta;
    private final URI endpoint;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(4)).build();

    MensajeriaFirebase(@Value("${iotelito.firebase.credenciales:}") String ruta,
                       @Value("${iotelito.firebase.project-id}") String proyecto,
                       ObjectMapper json) throws IOException {
        this.json = json;
        this.endpoint = URI.create("https://fcm.googleapis.com/v1/projects/" + proyecto + "/messages:send");
        if (ruta.isBlank()) {
            cuenta = null;
        } else {
            try (var archivo = Files.newInputStream(Path.of(ruta))) {
                cuenta = GoogleCredentials.fromStream(archivo)
                        .createScoped("https://www.googleapis.com/auth/firebase.messaging");
            }
        }
    }

    public boolean habilitada() {
        return cuenta != null;
    }

    public Resultado enviar(String token, Map<String, String> datos, boolean oferta) {
        try {
            cuenta.refreshIfExpired();
            var mensaje = Map.of("message", Map.of("token", token, "data", datos,
                    "android", Map.of("priority", "high", "ttl", oferta ? "900s" : "86400s")));
            var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(6))
                    .header("Authorization", "Bearer " + cuenta.getAccessToken().getTokenValue())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(mensaje))).build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return Resultado.ENVIADO;
            }
            for (var detalle : json.readTree(response.body()).path("error").path("details")) {
                if ("UNREGISTERED".equals(detalle.path("errorCode").asText())) {
                    return Resultado.TOKEN_REVOCADO;
                }
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (IOException error) {
            return Resultado.REINTENTAR;
        }
        return Resultado.REINTENTAR;
    }
}
