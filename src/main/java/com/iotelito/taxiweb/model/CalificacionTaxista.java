package com.iotelito.taxiweb.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Calificación del cliente al taxista: una por servicio, de 1 a 5. */
@Entity
@Table(name = "calificaciones_taxista")
public class CalificacionTaxista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_taxi_id", nullable = false, unique = true)
    private ServicioTaxi servicio;

    @Column(nullable = false, length = 128)
    private String clienteUid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "taxista_id", nullable = false)
    private Taxista taxista;

    @Column(nullable = false)
    private Integer puntuacion;

    @Column(length = 500)
    private String comentario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @PrePersist
    void marcarFecha() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }

    public ServicioTaxi getServicio() { return servicio; }

    public void setServicio(ServicioTaxi servicio) { this.servicio = servicio; }

    public String getClienteUid() { return clienteUid; }

    public void setClienteUid(String clienteUid) { this.clienteUid = clienteUid; }

    public Taxista getTaxista() { return taxista; }

    public void setTaxista(Taxista taxista) { this.taxista = taxista; }

    public Integer getPuntuacion() { return puntuacion; }

    public void setPuntuacion(Integer puntuacion) { this.puntuacion = puntuacion; }

    public String getComentario() { return comentario; }

    public void setComentario(String comentario) { this.comentario = comentario; }

    public LocalDateTime getFecha() { return fecha; }
}
