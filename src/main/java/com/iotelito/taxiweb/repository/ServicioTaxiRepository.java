package com.iotelito.taxiweb.repository;

import com.iotelito.taxiweb.model.EstadoServicioTaxi;
import com.iotelito.taxiweb.model.ServicioTaxi;
import com.iotelito.taxiweb.model.SolicitudTaxi;
import com.iotelito.taxiweb.model.Taxista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServicioTaxiRepository extends JpaRepository<ServicioTaxi, Long> {

    Optional<ServicioTaxi> findBySolicitud(SolicitudTaxi solicitud);

    Optional<ServicioTaxi> findFirstByTaxistaAndEstadoIn(Taxista taxista, Collection<EstadoServicioTaxi> estados);

    List<ServicioTaxi> findByTaxistaAndEstadoOrderByHoraFinDesc(Taxista taxista, EstadoServicioTaxi estado);
}
