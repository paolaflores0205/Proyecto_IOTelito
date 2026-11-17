package com.iotelito.taxiweb.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Punto enviado por la app del taxista cada 15 s durante un servicio activo. */
@Entity
@Table(name = "ubicaciones_taxista")
public class UbicacionTaxista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private ServicioTaxi servicio;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxista_id", nullable = false)
    private Taxista taxista;

    @Column(nullable = false)
    private Double latitud;

    @Column(nullable = false)
    private Double longitud;

    @Column(name = "precision_m")
    private Double precisionM;

    @Column(nullable = false)
    private LocalDateTime registradaEn;

    @PrePersist
    void marcarRegistro() {
        if (registradaEn == null) {
            registradaEn = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }

    public ServicioTaxi getServicio() { return servicio; }

    public void setServicio(ServicioTaxi servicio) { this.servicio = servicio; }

    public Taxista getTaxista() { return taxista; }

    public void setTaxista(Taxista taxista) { this.taxista = taxista; }

    public Double getLatitud() { return latitud; }

    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }

    public void setLongitud(Double longitud) { this.longitud = longitud; }

    public Double getPrecisionM() { return precisionM; }

    public void setPrecisionM(Double precisionM) { this.precisionM = precisionM; }

    public LocalDateTime getRegistradaEn() { return registradaEn; }
}
