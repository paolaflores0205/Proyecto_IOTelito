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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Servicio de taxi: existe desde que un taxista acepta el pedido. La base garantiza que un
 * pedido tenga un solo servicio y que un taxista no tenga dos servicios activos
 * (columna generada taxista_activo_id, que no se mapea aquí).
 */
@Entity
@Table(name = "servicios_taxi")
public class ServicioTaxi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false, unique = true)
    private SolicitudTaxi solicitud;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxista_id", nullable = false)
    private Taxista taxista;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoServicioTaxi estado = EstadoServicioTaxi.ASIGNADO;

    @Column(nullable = false)
    private LocalDateTime horaAceptacion;

    private LocalDateTime horaInicio;

    private LocalDateTime horaTraslado;

    private LocalDateTime horaFin;

    @Column(precision = 8, scale = 2)
    private BigDecimal distanciaRealKm;

    @Column(nullable = false, unique = true, length = 64)
    private String codigoQr;

    @Column(nullable = false)
    private Boolean qrValidado = false;

    public Long getId() { return id; }

    public SolicitudTaxi getSolicitud() { return solicitud; }

    public void setSolicitud(SolicitudTaxi solicitud) { this.solicitud = solicitud; }

    public Taxista getTaxista() { return taxista; }

    public void setTaxista(Taxista taxista) { this.taxista = taxista; }

    public EstadoServicioTaxi getEstado() { return estado; }

    public void setEstado(EstadoServicioTaxi estado) { this.estado = estado; }

    public LocalDateTime getHoraAceptacion() { return horaAceptacion; }

    public void setHoraAceptacion(LocalDateTime horaAceptacion) { this.horaAceptacion = horaAceptacion; }

    public LocalDateTime getHoraInicio() { return horaInicio; }

    public void setHoraInicio(LocalDateTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalDateTime getHoraTraslado() { return horaTraslado; }

    public void setHoraTraslado(LocalDateTime horaTraslado) { this.horaTraslado = horaTraslado; }

    public LocalDateTime getHoraFin() { return horaFin; }

    public void setHoraFin(LocalDateTime horaFin) { this.horaFin = horaFin; }

    public BigDecimal getDistanciaRealKm() { return distanciaRealKm; }

    public void setDistanciaRealKm(BigDecimal distanciaRealKm) { this.distanciaRealKm = distanciaRealKm; }

    public String getCodigoQr() { return codigoQr; }

    public void setCodigoQr(String codigoQr) { this.codigoQr = codigoQr; }

    public Boolean getQrValidado() { return qrValidado; }

    public void setQrValidado(Boolean qrValidado) { this.qrValidado = qrValidado; }
}
