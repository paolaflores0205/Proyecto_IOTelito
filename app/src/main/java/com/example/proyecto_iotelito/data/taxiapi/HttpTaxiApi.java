package com.example.proyecto_iotelito.data.taxiapi;

import org.json.*;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/** HTTPS sin dependencias HTTP. Un plazo total incluye token, lectura y único reintento. */
public final class HttpTaxiApi extends JsonTaxiApi {
    public interface Tokens {
        String obtener(boolean renovar, long restanteMs) throws Exception;
    }

    private final String base;
    private final Tokens tokens;
    private final int timeoutMs;

    public HttpTaxiApi(String base, Tokens tokens) {
        this(base, tokens, 10000, false);
    }

    // Solo las pruebas del mismo paquete pueden usar HTTP loopback.
    HttpTaxiApi(String base, Tokens tokens, int timeoutMs, boolean loopbackTest) {
        this.tokens = tokens;
        this.timeoutMs = timeoutMs;
        try {
            URI uri = URI.create(base);
            boolean local =
                    loopbackTest
                            && "http".equals(uri.getScheme())
                            && "127.0.0.1".equals(uri.getHost());
            if ((!"https".equals(uri.getScheme()) && !local)
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null) throw new IllegalArgumentException();
            this.base = base.replaceAll("/+$", "");
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Configura taxiApi.url con la base HTTPS de la API, incluyendo /api/v1.");
        }
    }

    private int restante(long deadline) {
        long ms = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
        if (ms <= 0) throw TaxiApiException.noDisponible();
        return (int) Math.min(ms, Integer.MAX_VALUE);
    }

    @Override
    protected JSONObject request(String metodo, String ruta, JSONObject body, boolean autenticada) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        for (int intento = 0; intento < 2; intento++) {
            HttpURLConnection c = null;
            try {
                String token =
                        autenticada ? tokens.obtener(intento == 1, restante(deadline)) : null;
                if (autenticada && (token == null || token.isEmpty()))
                    throw new TaxiApiException(
                            401,
                            TaxiApiException.Codigo.NO_AUTENTICADO,
                            "Inicia sesión nuevamente.");
                c = (HttpURLConnection) new URL(base + ruta).openConnection();
                c.setInstanceFollowRedirects(false);
                c.setRequestMethod(metodo);
                c.setConnectTimeout(restante(deadline));
                c.setReadTimeout(restante(deadline));
                c.setRequestProperty("Accept", "application/json");
                if (autenticada) c.setRequestProperty("Authorization", "Bearer " + token);
                if (body != null) {
                    byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                    c.setDoOutput(true);
                    c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                    c.setFixedLengthStreamingMode(bytes.length);
                    try (OutputStream out = c.getOutputStream()) {
                        out.write(bytes);
                    }
                }
                int status = c.getResponseCode();
                if (status == 401 && autenticada && intento == 0) continue;
                if (status == 204) return null;
                InputStream stream =
                        status >= 200 && status < 300 ? c.getInputStream() : c.getErrorStream();
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                if (stream != null)
                    try (InputStream in = stream) {
                        byte[] buffer = new byte[4096];
                        int n;
                        while (true) {
                            c.setReadTimeout(restante(deadline));
                            n = in.read(buffer);
                            if (n < 0) break;
                            bytes.write(buffer, 0, n);
                            if (bytes.size() > 1048576) throw TaxiApiException.noDisponible();
                        }
                    }
                JSONObject response = new JSONObject(bytes.toString("UTF-8"));
                if (status >= 200 && status < 300) return response;
                TaxiApiException.Codigo codigo;
                try {
                    codigo = TaxiApiException.Codigo.valueOf(response.getString("codigo"));
                } catch (Exception invalid) {
                    throw TaxiApiException.noDisponible();
                }
                throw new TaxiApiException(status, codigo, response.getString("mensaje"));
            } catch (TaxiApiException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw TaxiApiException.noDisponible();
            } catch (Exception e) {
                throw TaxiApiException.noDisponible();
            } finally {
                if (c != null) c.disconnect();
            }
        }
        throw TaxiApiException.noDisponible();
    }
}
