package com.iotelito.taxiweb.repository;

import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.model.EstadoSolicitudTaxista;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TaxistaRepository
        extends JpaRepository<Taxista, Long> {

    boolean existsByDni(String dni);

    boolean existsByNumeroLicencia(String numeroLicencia);

    boolean existsByPlacaVehiculo(String placaVehiculo);

    long countByEstadoSolicitud(EstadoSolicitudTaxista estadoSolicitud);

    long countByUsuarioActivoTrue();

    long countByUsuarioActivoFalse();

    List<Taxista> findByEstadoSolicitudOrderByFechaSolicitudDesc(
            EstadoSolicitudTaxista estadoSolicitud
    );

    List<Taxista> findByDisponibleOrderByFechaSolicitudDesc(Boolean disponible);

    List<Taxista> findAllByOrderByFechaSolicitudDesc();

    List<Taxista> findByDniContainingIgnoreCaseOrPlacaVehiculoContainingIgnoreCaseOrUsuarioNombresContainingIgnoreCaseOrUsuarioApellidosContainingIgnoreCaseOrderByFechaSolicitudDesc(
            String dni,
            String placaVehiculo,
            String nombres,
            String apellidos
    );

    Page<Taxista> findByEstadoSolicitud(
            EstadoSolicitudTaxista estadoSolicitud,
            Pageable pageable
    );

    Page<Taxista> findByDisponible(Boolean disponible, Pageable pageable);

    Page<Taxista> findByDniContainingIgnoreCaseOrPlacaVehiculoContainingIgnoreCaseOrUsuarioNombresContainingIgnoreCaseOrUsuarioApellidosContainingIgnoreCase(
            String dni,
            String placaVehiculo,
            String nombres,
            String apellidos,
            Pageable pageable
    );

    @Query("""
            select t
            from Taxista t
            where t.estadoSolicitud = :estado
              and (
                    lower(t.dni) like lower(concat('%', :termino, '%'))
                 or lower(t.placaVehiculo) like lower(concat('%', :termino, '%'))
                 or lower(t.usuario.nombres) like lower(concat('%', :termino, '%'))
                 or lower(t.usuario.apellidos) like lower(concat('%', :termino, '%'))
              )
            """)
    Page<Taxista> buscarPorEstadoYTermino(
            EstadoSolicitudTaxista estado,
            String termino,
            Pageable pageable
    );
}
