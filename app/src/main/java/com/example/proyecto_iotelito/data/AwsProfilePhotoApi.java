package com.example.proyecto_iotelito.data;

import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.UUID;
import javax.net.ssl.HttpsURLConnection;

/** Transporte privado: el token Firebase solo se envía a nuestra API, nunca a S3. */
final class AwsProfilePhotoApi {
    private AwsProfilePhotoApi() { }
    static final class PhotoException extends IOException {
        PhotoException(String message) { super(message); }
    }
    static JSONObject solicitar(String route, String token, JSONObject body) throws Exception {
        HttpsURLConnection connection = abrir(ProfilePhotoPolicy.API + route);
        try {
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + token);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            byte[] json = body.toString().getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(json.length);
            try (OutputStream out = connection.getOutputStream()) { out.write(json); }
            int status = connection.getResponseCode();
            if (status != 200) throw error(status);
            try (InputStream in = connection.getInputStream()) {
                return new JSONObject(new String(leer(in, 65536), StandardCharsets.UTF_8));
            }
        } finally { connection.disconnect(); }
    }
    static void subir(File file, JSONObject signed) throws Exception {
        String url = signed.getString("url");
        if (!ProfilePhotoPolicy.destinoS3(url) || !"POST".equals(signed.getString("method")))
            throw new PhotoException("No se pudo preparar la subida de la foto. Reintenta.");
        JSONObject fields = signed.getJSONObject("fields");
        String boundary = "IotTelito" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream preamble = new ByteArrayOutputStream();
        Iterator<String> names = fields.keys();
        while (names.hasNext()) {
            String name = names.next();
            if (!name.matches("[A-Za-z0-9_-]+")) throw new PhotoException("Respuesta de subida inválida.");
            escribir(preamble, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name
                    + "\"\r\n\r\n" + fields.getString(name) + "\r\n");
        }
        escribir(preamble, "--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"foto.jpg\""
                + "\r\nContent-Type: image/jpeg\r\n\r\n");
        byte[] end = ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8);
        long size = file.length();
        if (!file.isFile() || size < 1 || size > ProfilePhotoPolicy.MAX_BYTES)
            throw new PhotoException("Selecciona nuevamente una foto de menos de 2 MiB.");
        HttpsURLConnection connection = abrir(url);
        try {
            connection.setRequestMethod("POST"); connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            connection.setFixedLengthStreamingMode(preamble.size() + size + end.length);
            try (OutputStream out = connection.getOutputStream(); InputStream in = new FileInputStream(file)) {
                preamble.writeTo(out); byte[] buffer = new byte[8192]; int n; long copied = 0;
                while ((n = in.read(buffer)) != -1) {
                    copied += n; if (copied > size) throw new IOException("La foto cambió durante la subida.");
                    out.write(buffer, 0, n);
                }
                if (copied != size) throw new IOException("La foto cambió durante la subida.");
                out.write(end);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300)
                throw new PhotoException("No se pudo subir la foto. Reintenta; tus cambios siguen en el formulario.");
        } finally { connection.disconnect(); }
    }
    static byte[] descargar(String url) throws Exception {
        if (!ProfilePhotoPolicy.destinoS3(url)) throw new PhotoException("Enlace de foto inválido.");
        HttpsURLConnection connection = abrir(url);
        try {
            if (connection.getResponseCode() != 200) throw new PhotoException("No se pudo cargar la foto.");
            if (connection.getContentLengthLong() > ProfilePhotoPolicy.MAX_BYTES)
                throw new PhotoException("La foto supera el tamaño permitido.");
            try (InputStream in = connection.getInputStream()) { return leer(in, ProfilePhotoPolicy.MAX_BYTES); }
        } finally { connection.disconnect(); }
    }
    private static HttpsURLConnection abrir(String url) throws IOException {
        HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000); connection.setReadTimeout(20000);
        connection.setInstanceFollowRedirects(false); connection.setUseCaches(false);
        return connection;
    }
    private static byte[] leer(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(); byte[] buffer = new byte[8192]; int n;
        while ((n = in.read(buffer)) != -1) {
            if (out.size() + n > limit) throw new PhotoException("La respuesta supera el tamaño permitido.");
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }
    private static void escribir(OutputStream out, String value) throws IOException {
        out.write(value.getBytes(StandardCharsets.UTF_8));
    }
    private static PhotoException error(int status) {
        if (status == 401) return new PhotoException("Tu sesión venció. Inicia sesión nuevamente para guardar la foto.");
        if (status == 403) return new PhotoException("No se pudo autorizar tu foto. Comprueba que tu perfil esté habilitado.");
        if (status == 400) return new PhotoException("Selecciona una foto JPEG de menos de 2 MiB.");
        return new PhotoException("No se pudo acceder al servicio de fotos. Reintenta en un momento.");
    }
}
