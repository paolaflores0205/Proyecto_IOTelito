package com.iotelito.taxiweb.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pedido de taxi de una reserva. Hotel, cliente y reserva viven en Firestore: aquí se
 * guardan sus ids y una copia de los datos que necesita el taxista.
 */
@Entity
@Table(name = "solicitudes_taxi")
public class SolicitudTaxi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long reservaId;

    @Column(nullable = false, length = 128)
    private String clienteUid;

    @Column(nullable = false, length = 160)
    private String clienteNombre;

    @Column(nullable = false)
    private Long hotelId;

    @Column(nullable = false, length = 160)
    private String hotelNombre;

    @Column(nullable = false, length = 120)
    private String hotelCiudad;

    @Column(nullable = false, length = 500)
    private String origen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CodigoAeropuerto aeropuertoCodigo;

    @Column(nullable = false)
    private String destino;

    @Column(nullable = false, length = 5)
    private String horaRecojo;

    @Column(nullable = false)
    private Integer cantidadPasajeros;

    @Column(nullable = false)
    private Integer cantidadEquipaje = 0;

    @Column(precision = 8, scale = 2)
    private BigDecimal distanciaKm;

    private Integer duracionEstimadaMin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitudTaxi estado = EstadoSolicitudTaxi.PENDIENTE;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    @PrePersist
    void marcarSolicitud() {
        if (fechaSolicitud == null) {
            fechaSolicitud = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }

    public Long getReservaId() { return reservaId; }

    public void setReservaId(Long reservaId) { this.reservaId = reservaId; }

    public String getClienteUid() { return clienteUid; }

    public void setClienteUid(String clienteUid) { this.clienteUid = clienteUid; }

    public String getClienteNombre() { return clienteNombre; }

    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }

    public Long getHotelId() { return hotelId; }

    public void setHotelId(Long hotelId) { this.hotelId = hotelId; }

    public String getHotelNombre() { return hotelNombre; }

    public void setHotelNombre(String hotelNombre) { this.hotelNombre = hotelNombre; }

    public String getHotelCiudad() { return hotelCiudad; }

    public void setHotelCiudad(String hotelCiudad) { this.hotelCiudad = hotelCiudad; }

    public String getOrigen() { return origen; }

    public void setOrigen(String origen) { this.origen = origen; }

    public CodigoAeropuerto getAeropuertoCodigo() { return aeropuertoCodigo; }

    public void setAeropuertoCodigo(CodigoAeropuerto aeropuertoCodigo) { this.aeropuertoCodigo = aeropuertoCodigo; }

    public String getDestino() { return destino; }

    public void setDestino(String destino) { this.destino = destino; }

    public String getHoraRecojo() { return horaRecojo; }

    public void setHoraRecojo(String horaRecojo) { this.horaRecojo = horaRecojo; }

    public Integer getCantidadPasajeros() { return cantidadPasajeros; }

    public void setCantidadPasajeros(Integer cantidadPasajeros) { this.cantidadPasajeros = cantidadPasajeros; }

    public Integer getCantidadEquipaje() { return cantidadEquipaje; }

    public void setCantidadEquipaje(Integer cantidadEquipaje) { this.cantidadEquipaje = cantidadEquipaje; }

    public BigDecimal getDistanciaKm() { return distanciaKm; }

    public void setDistanciaKm(BigDecimal distanciaKm) { this.distanciaKm = distanciaKm; }

    public Integer getDuracionEstimadaMin() { return duracionEstimadaMin; }

    public void setDuracionEstimadaMin(Integer duracionEstimadaMin) { this.duracionEstimadaMin = duracionEstimadaMin; }

    public EstadoSolicitudTaxi getEstado() { return estado; }

    public void setEstado(EstadoSolicitudTaxi estado) { this.estado = estado; }

    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
}
