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

/** Evento relevante del sistema web: aprobaciones, rechazos y activaciones. */
@Entity
@Table(name = "logs_sistema")
public class LogSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(nullable = false)
    private String accion;

    @Column(length = 2000)
    private String detalle;

    @Column(nullable = false)
    private LocalDateTime fecha;

    public LogSistema() {
    }

    public LogSistema(Usuario usuario, String accion, String detalle) {
        this.usuario = usuario;
        this.accion = accion;
        this.detalle = detalle;
    }

    @PrePersist
    void marcarFecha() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }

    public Usuario getUsuario() { return usuario; }

    public String getAccion() { return accion; }

    public String getDetalle() { return detalle; }

    public LocalDateTime getFecha() { return fecha; }
}
