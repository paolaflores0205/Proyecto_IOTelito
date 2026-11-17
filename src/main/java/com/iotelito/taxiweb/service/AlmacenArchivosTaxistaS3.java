package com.iotelito.taxiweb.service;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.time.Duration;

@Component
class AlmacenArchivosTaxistaS3 {

    private final String bucket;
    private final S3Client cliente;
    private final S3Presigner firmador;

    AlmacenArchivosTaxistaS3(
            @Value("${iotelito.taxistas.s3.bucket:}") String bucket,
            @Value("${iotelito.taxistas.s3.region:us-east-1}") String region
    ) {
        this.bucket = bucket.strip();
        if (habilitado()) {
            Region regionS3 = Region.of(region);
            cliente = S3Client.builder()
                    .region(regionS3)
                    .httpClientBuilder(UrlConnectionHttpClient.builder())
                    .overrideConfiguration(config -> config
                            .apiCallTimeout(Duration.ofSeconds(15))
                            .apiCallAttemptTimeout(Duration.ofSeconds(7)))
                    .build();
            firmador = S3Presigner.builder().region(regionS3).build();
        } else {
            cliente = null;
            firmador = null;
        }
    }

    boolean habilitado() {
        return !bucket.isEmpty();
    }

    void guardar(String clave, MultipartFile archivo, String contentType) {
        cliente.putObject(request -> request.bucket(bucket).key(clave).contentType(contentType),
                RequestBody.fromContentProvider(() -> {
                    try {
                        return archivo.getInputStream();
                    } catch (IOException error) {
                        throw new UncheckedIOException(error);
                    }
                }, archivo.getSize(), contentType));
    }

    URI enlaceTemporal(String clave, String contentType) {
        GetObjectRequest solicitud = GetObjectRequest.builder()
                .bucket(bucket)
                .key(clave)
                .responseContentType(contentType)
                .responseContentDisposition(contentType.equals("application/pdf") ? "attachment" : "inline")
                .responseCacheControl("private, no-store")
                .build();
        return URI.create(firmador.presignGetObject(request -> request
                .signatureDuration(Duration.ofMinutes(5))
                .getObjectRequest(solicitud)).url().toExternalForm());
    }

    void eliminar(String clave) {
        cliente.deleteObject(request -> request.bucket(bucket).key(clave));
    }

    @PreDestroy
    void cerrar() {
        if (habilitado()) {
            firmador.close();
            cliente.close();
        }
    }
}
