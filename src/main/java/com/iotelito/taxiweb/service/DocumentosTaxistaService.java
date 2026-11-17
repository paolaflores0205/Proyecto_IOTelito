package com.iotelito.taxiweb.service;

import com.iotelito.taxiweb.dto.TaxistaRegistroForm;
import com.iotelito.taxiweb.model.DocumentoTaxista;
import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.model.TipoArchivoTaxista;
import com.iotelito.taxiweb.repository.DocumentoTaxistaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class DocumentosTaxistaService {

    private static final Logger LOG = LoggerFactory.getLogger(DocumentosTaxistaService.class);
    private static final long MIB = 1024 * 1024;
    private static final Pattern NOMBRE_OBJETO = Pattern.compile(
            "[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}\\.(jpg|png|pdf)");
    private static final byte[] FIRMA_JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff};
    private static final byte[] FIRMA_PNG = {(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a};
    private static final byte[] FIRMA_PDF = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private final DocumentoTaxistaRepository documentos;
    private final AlmacenArchivosTaxistaS3 almacenamiento;

    DocumentosTaxistaService(DocumentoTaxistaRepository documentos, AlmacenArchivosTaxistaS3 almacenamiento) {
        this.documentos = documentos;
        this.almacenamiento = almacenamiento;
    }

    List<ArchivoValidado> validar(TaxistaRegistroForm form) {
        List<ArchivoValidado> archivos = new ArrayList<>();
        for (TipoArchivoTaxista tipo : TipoArchivoTaxista.values()) {
            MultipartFile archivo = switch (tipo) {
                case FOTO_CONDUCTOR -> form.getFotoConductorFile();
                case FOTO_VEHICULO -> form.getFotoVehiculoFile();
                case DOCUMENTO_IDENTIDAD -> form.getDniFile();
                case LICENCIA -> form.getLicenciaFile();
                case SOAT -> form.getSoatFile();
                case TARJETA_PROPIEDAD -> form.getTarjetaPropiedadFile();
            };
            if (archivo == null || (archivo.isEmpty()
                    && (archivo.getOriginalFilename() == null || archivo.getOriginalFilename().isBlank()))) {
                continue;
            }

            int maximo = esFoto(tipo) ? 2 : 5;
            if (archivo.isEmpty() || archivo.getSize() > maximo * MIB) {
                throw new IllegalArgumentException("El archivo para " + tipo.getEtiqueta()
                        + " debe pesar entre 1 byte y " + maximo + " MB.");
            }
            archivos.add(new ArchivoValidado(tipo, archivo, formato(tipo, archivo)));
        }
        if (archivos.stream().mapToLong(a -> a.archivo().getSize()).sum() > 8 * MIB) {
            throw new IllegalArgumentException("La suma de todos los archivos debe ser de 8 MB o menos.");
        }
        if (!archivos.isEmpty() && !almacenamiento.habilitado()) {
            throw new IllegalStateException("La carga de archivos no está disponible. Intenta de nuevo más tarde.");
        }
        return archivos;
    }

    void guardar(Taxista taxista, List<ArchivoValidado> archivos) {
        if (archivos.isEmpty()) {
            return;
        }

        List<String> claves = new ArrayList<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int estado) {
                if (estado == STATUS_ROLLED_BACK) {
                    claves.forEach(DocumentosTaxistaService.this::eliminarTrasRollback);
                }
            }
        });

        for (ArchivoValidado archivo : archivos) {
            Formato formato = archivo.formato();
            String clave = prefijo(taxista.getId(), archivo.tipo()) + UUID.randomUUID() + "." + formato.extension;
            claves.add(clave);
            try {
                almacenamiento.guardar(clave, archivo.archivo(), formato.contentType);
            } catch (RuntimeException error) {
                LOG.warn("Falló la carga de un adjunto de taxista ({})", error.getClass().getSimpleName());
                throw new IllegalStateException("No pudimos guardar los archivos. Vuelve a seleccionar los adjuntos e intenta de nuevo.");
            }

            DocumentoTaxista documento = new DocumentoTaxista();
            documento.setTaxista(taxista);
            documento.setTipo(archivo.tipo());
            documento.setArchivoUrl(clave);
            documento.setContentType(formato.contentType);
            documento.setTamanoBytes(Math.toIntExact(archivo.archivo().getSize()));
            documentos.save(documento);
        }
    }

    public List<DocumentoTaxista> listarAdjuntos(Taxista taxista) {
        return documentos.findByTaxistaOrderByTipo(taxista).stream()
                .filter(documento -> tieneObjeto(taxista.getId(), documento))
                .toList();
    }

    public Optional<URI> abrir(Long taxistaId, Long documentoId) {
        return documentos.findByIdAndTaxistaId(documentoId, taxistaId)
                .filter(documento -> tieneObjeto(taxistaId, documento))
                .map(this::firmar);
    }

    public record Fotos(String conductor, String vehiculo) {}

    public Fotos fotos(Taxista taxista) {
        if (!almacenamiento.habilitado()) {
            return new Fotos(null, null);
        }
        String conductor = null;
        String vehiculo = null;
        for (DocumentoTaxista documento : listarAdjuntos(taxista)) {
            if (esFoto(documento.getTipo())) {
                try {
                    String enlace = firmar(documento).toString();
                    if (documento.getTipo() == TipoArchivoTaxista.FOTO_CONDUCTOR) {
                        conductor = enlace;
                    } else {
                        vehiculo = enlace;
                    }
                } catch (IllegalStateException error) {
                    return new Fotos(conductor, vehiculo);
                }
            }
        }
        return new Fotos(conductor, vehiculo);
    }

    private URI firmar(DocumentoTaxista documento) {
        if (!almacenamiento.habilitado()) {
            throw new IllegalStateException("La consulta de archivos no está disponible.");
        }
        try {
            return almacenamiento.enlaceTemporal(documento.getArchivoUrl(), documento.getContentType());
        } catch (RuntimeException error) {
            LOG.warn("Falló la firma de un enlace de taxista ({})", error.getClass().getSimpleName());
            throw new IllegalStateException("La consulta de archivos no está disponible.");
        }
    }

    private void eliminarTrasRollback(String clave) {
        try {
            almacenamiento.eliminar(clave);
        } catch (RuntimeException error) {
            LOG.warn("No se pudo limpiar el objeto {} de un registro revertido ({})",
                    clave, error.getClass().getSimpleName());
        }
    }

    private static Formato formato(TipoArchivoTaxista tipo, MultipartFile archivo) {
        try (InputStream entrada = archivo.getInputStream()) {
            byte[] inicio = entrada.readNBytes(FIRMA_PNG.length);
            if (empiezaCon(inicio, FIRMA_JPEG)) {
                return Formato.JPEG;
            }
            if (Arrays.equals(inicio, FIRMA_PNG)) {
                return Formato.PNG;
            }
            if (!esFoto(tipo) && empiezaCon(inicio, FIRMA_PDF)) {
                return Formato.PDF;
            }
        } catch (IOException error) {
            throw new IllegalArgumentException("No se pudo leer el archivo de " + tipo.getEtiqueta() + ".");
        }
        throw new IllegalArgumentException("El archivo para " + tipo.getEtiqueta() + " debe ser "
                + (esFoto(tipo) ? "JPG o PNG." : "PDF, JPG o PNG."));
    }

    private static boolean empiezaCon(byte[] contenido, byte[] firma) {
        return contenido.length >= firma.length
                && Arrays.equals(contenido, 0, firma.length, firma, 0, firma.length);
    }

    private static boolean esFoto(TipoArchivoTaxista tipo) {
        return tipo == TipoArchivoTaxista.FOTO_CONDUCTOR || tipo == TipoArchivoTaxista.FOTO_VEHICULO;
    }

    private static String prefijo(Long taxistaId, TipoArchivoTaxista tipo) {
        return "taxistaDocs/" + taxistaId + "/" + tipo.name().toLowerCase(Locale.ROOT) + "/";
    }

    private static boolean tieneObjeto(Long taxistaId, DocumentoTaxista documento) {
        String clave = documento.getArchivoUrl();
        String prefijo = prefijo(taxistaId, documento.getTipo());
        if (!clave.startsWith(prefijo) || !NOMBRE_OBJETO.matcher(clave.substring(prefijo.length())).matches()) {
            return false;
        }
        return switch (documento.getContentType() == null ? "" : documento.getContentType()) {
            case "image/jpeg" -> clave.endsWith(".jpg");
            case "image/png" -> clave.endsWith(".png");
            case "application/pdf" -> !esFoto(documento.getTipo()) && clave.endsWith(".pdf");
            default -> false;
        };
    }

    record ArchivoValidado(TipoArchivoTaxista tipo, MultipartFile archivo, Formato formato) {}

    private enum Formato {
        JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), PDF("pdf", "application/pdf");

        private final String extension;
        private final String contentType;

        Formato(String extension, String contentType) {
            this.extension = extension;
            this.contentType = contentType;
        }
    }
}
