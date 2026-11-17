package com.iotelito.taxiweb.repository;

import com.iotelito.taxiweb.model.EstadoSolicitudTaxi;
import com.iotelito.taxiweb.model.SolicitudTaxi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SolicitudTaxiRepository extends JpaRepository<SolicitudTaxi, Long> {

    Optional<SolicitudTaxi> findByReservaId(Long reservaId);

    List<SolicitudTaxi> findByEstadoOrderByHoraRecojoAsc(EstadoSolicitudTaxi estado);
}
