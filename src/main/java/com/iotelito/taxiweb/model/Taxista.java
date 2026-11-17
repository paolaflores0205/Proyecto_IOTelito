package com.iotelito.taxiweb.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "taxistas")
public class Taxista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false, length = 12)
    private String numeroDocumento;

    @Column(nullable = false)
    private LocalDate fechaNacimiento;

    @Column(nullable = false)
    private String domicilio;

    @Column(nullable = false, unique = true, length = 50)
    private String numeroLicencia;

    @Column(nullable = false, length = 20)
    private String categoriaLicencia;

    @Column(nullable = false)
    private LocalDate fechaVencimientoLicencia;

    @Column(nullable = false, length = 60)
    private String marcaVehiculo;

    @Column(nullable = false, length = 60)
    private String modeloVehiculo;

    @Column(nullable = false)
    private Integer anioVehiculo;

    @Column(nullable = false, length = 50)
    private String colorVehiculo;

    @Column(nullable = false, unique = true, length = 20)
    private String placaVehiculo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitudTaxista estadoSolicitud = EstadoSolicitudTaxista.PENDIENTE;

    @Column(nullable = false)
    private Boolean disponible = false;

    @Column(nullable = false, precision = 3, scale = 2)
    private BigDecimal calificacion = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer cantidadServicios = 0;

    @Column(nullable = false)
    private Boolean aceptaTerminos = false;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    @PrePersist
    public void marcarSolicitud() {
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
        }
    }

    public Taxista() {
    }

    /** Para mostrar en el panel: "DNI 71234567", "Pasaporte PE4321698"... */
    public String getDocumento() {
        if (tipoDocumento == null) {
            return numeroDocumento;
        }
        return tipoDocumento.getEtiqueta() + " " + numeroDocumento;
    }

    public Long getId() { return id; }

    public void setId(Long id) { this.id = id; }

    public Usuario getUsuario() { return usuario; }

    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public TipoDocumento getTipoDocumento() { return tipoDocumento; }

    public void setTipoDocumento(TipoDocumento tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public String getNumeroDocumento() { return numeroDocumento; }

    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }

    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public String getDomicilio() { return domicilio; }

    public void setDomicilio(String domicilio) { this.domicilio = domicilio; }

    public String getNumeroLicencia() { return numeroLicencia; }

    public void setNumeroLicencia(String numeroLicencia) { this.numeroLicencia = numeroLicencia; }

    public String getCategoriaLicencia() { return categoriaLicencia; }

    public void setCategoriaLicencia(String categoriaLicencia) { this.categoriaLicencia = categoriaLicencia; }

    public LocalDate getFechaVencimientoLicencia() { return fechaVencimientoLicencia; }

    public void setFechaVencimientoLicencia(LocalDate fechaVencimientoLicencia) {
        this.fechaVencimientoLicencia = fechaVencimientoLicencia;
    }

    public String getMarcaVehiculo() { return marcaVehiculo; }

    public void setMarcaVehiculo(String marcaVehiculo) { this.marcaVehiculo = marcaVehiculo; }

    public String getModeloVehiculo() { return modeloVehiculo; }

    public void setModeloVehiculo(String modeloVehiculo) { this.modeloVehiculo = modeloVehiculo; }

    public Integer getAnioVehiculo() { return anioVehiculo; }

    public void setAnioVehiculo(Integer anioVehiculo) { this.anioVehiculo = anioVehiculo; }

    public String getColorVehiculo() { return colorVehiculo; }

    public void setColorVehiculo(String colorVehiculo) { this.colorVehiculo = colorVehiculo; }

    public String getPlacaVehiculo() { return placaVehiculo; }

    public void setPlacaVehiculo(String placaVehiculo) { this.placaVehiculo = placaVehiculo; }

    public EstadoSolicitudTaxista getEstadoSolicitud() { return estadoSolicitud; }

    public void setEstadoSolicitud(EstadoSolicitudTaxista estadoSolicitud) { this.estadoSolicitud = estadoSolicitud; }

    public Boolean getDisponible() { return disponible; }

    public void setDisponible(Boolean disponible) { this.disponible = disponible; }

    public BigDecimal getCalificacion() { return calificacion; }

    public void setCalificacion(BigDecimal calificacion) { this.calificacion = calificacion; }

    public Integer getCantidadServicios() { return cantidadServicios; }

    public void setCantidadServicios(Integer cantidadServicios) { this.cantidadServicios = cantidadServicios; }

    public Boolean getAceptaTerminos() { return aceptaTerminos; }

    public void setAceptaTerminos(Boolean aceptaTerminos) { this.aceptaTerminos = aceptaTerminos; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
}
