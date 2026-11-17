package com.iotelito.taxiweb.repository;

import com.iotelito.taxiweb.model.CalificacionTaxista;
import com.iotelito.taxiweb.model.ServicioTaxi;
import com.iotelito.taxiweb.model.Taxista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface CalificacionTaxistaRepository extends JpaRepository<CalificacionTaxista, Long> {

    Optional<CalificacionTaxista> findByServicio(ServicioTaxi servicio);

    @Query("select avg(c.puntuacion) from CalificacionTaxista c where c.taxista = :taxista")
    Double promedio(Taxista taxista);
}
