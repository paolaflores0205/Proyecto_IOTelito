package com.iotelito.taxiweb.repository;

import com.iotelito.taxiweb.model.EstadoServicioTaxi;
import com.iotelito.taxiweb.model.ServicioTaxi;
import com.iotelito.taxiweb.model.SolicitudTaxi;
import com.iotelito.taxiweb.model.Taxista;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ServicioTaxiRepository extends JpaRepository<ServicioTaxi, Long> {

    Optional<ServicioTaxi> findBySolicitud(SolicitudTaxi solicitud);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ServicioTaxi> findLockedBySolicitud(SolicitudTaxi solicitud);

    Optional<ServicioTaxi> findFirstByTaxistaAndEstadoIn(Taxista taxista, Collection<EstadoServicioTaxi> estados);

    List<ServicioTaxi> findByTaxistaAndEstadoOrderByHoraFinDesc(Taxista taxista, EstadoServicioTaxi estado, Pageable pagina);

    long countByTaxistaAndEstado(Taxista taxista, EstadoServicioTaxi estado);
}
