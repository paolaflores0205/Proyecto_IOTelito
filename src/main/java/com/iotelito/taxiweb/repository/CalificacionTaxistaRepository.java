package com.iotelito.taxiweb.repository;

import com.iotelito.taxiweb.model.CalificacionTaxista;
import com.iotelito.taxiweb.model.ServicioTaxi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CalificacionTaxistaRepository extends JpaRepository<CalificacionTaxista, Long> {

    Optional<CalificacionTaxista> findByServicio(ServicioTaxi servicio);
}
