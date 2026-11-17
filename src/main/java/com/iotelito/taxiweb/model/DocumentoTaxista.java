package com.iotelito.taxiweb.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Archivo del taxista. {@code archivoUrl} es la clave del objeto en el bucket privado de S3. */
@Entity
@Table(name = "documentos_taxista")
public class DocumentoTaxista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxista_id", nullable = false)
    private Taxista taxista;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoArchivoTaxista tipo;

    @Column(nullable = false, length = 500)
    private String archivoUrl;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private Integer tamanoBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDocumento estado = EstadoDocumento.PENDIENTE;

    @Column(length = 500)
    private String observacion;

    @Column(nullable = false)
    private LocalDateTime fechaSubida;

    @PrePersist
    void marcarSubida() {
        if (fechaSubida == null) {
            fechaSubida = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }

    public Taxista getTaxista() { return taxista; }

    public void setTaxista(Taxista taxista) { this.taxista = taxista; }

    public TipoArchivoTaxista getTipo() { return tipo; }

    public void setTipo(TipoArchivoTaxista tipo) { this.tipo = tipo; }

    public String getArchivoUrl() { return archivoUrl; }

    public void setArchivoUrl(String archivoUrl) { this.archivoUrl = archivoUrl; }

    public String getContentType() { return contentType; }

    public void setContentType(String contentType) { this.contentType = contentType; }

    public Integer getTamanoBytes() { return tamanoBytes; }

    public void setTamanoBytes(Integer tamanoBytes) { this.tamanoBytes = tamanoBytes; }

    public EstadoDocumento getEstado() { return estado; }

    public void setEstado(EstadoDocumento estado) { this.estado = estado; }

    public String getObservacion() { return observacion; }

    public void setObservacion(String observacion) { this.observacion = observacion; }

    public LocalDateTime getFechaSubida() { return fechaSubida; }
}
