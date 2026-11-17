package com.iotelito.taxiweb.repository;

import com.iotelito.taxiweb.model.ServicioTaxi;
import com.iotelito.taxiweb.model.UbicacionTaxista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UbicacionTaxistaRepository extends JpaRepository<UbicacionTaxista, Long> {

    Optional<UbicacionTaxista> findFirstByServicioOrderByRegistradaEnDesc(ServicioTaxi servicio);
}
