package com.iotelito.taxiweb.repository;

import com.iotelito.taxiweb.model.EstadoSolicitudTaxista;
import com.iotelito.taxiweb.model.Taxista;
import com.iotelito.taxiweb.model.TipoDocumento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TaxistaRepository
        extends JpaRepository<Taxista, Long> {

    boolean existsByTipoDocumentoAndNumeroDocumento(TipoDocumento tipoDocumento, String numeroDocumento);

    boolean existsByNumeroLicencia(String numeroLicencia);

    boolean existsByPlacaVehiculo(String placaVehiculo);

    Optional<Taxista> findByUsuarioFirebaseUid(String firebaseUid);

    long countByEstadoSolicitud(EstadoSolicitudTaxista estadoSolicitud);

    long countByUsuarioActivoTrue();

    long countByUsuarioActivoFalse();

    List<Taxista> findByEstadoSolicitudOrderByFechaSolicitudDesc(
            EstadoSolicitudTaxista estadoSolicitud
    );

    List<Taxista> findByDisponibleOrderByFechaSolicitudDesc(Boolean disponible);

    List<Taxista> findAllByOrderByFechaSolicitudDesc();

    @Query("""
            select t
            from Taxista t
            where lower(t.numeroDocumento) like lower(concat('%', :termino, '%'))
               or lower(t.placaVehiculo) like lower(concat('%', :termino, '%'))
               or lower(t.usuario.nombres) like lower(concat('%', :termino, '%'))
               or lower(t.usuario.apellidos) like lower(concat('%', :termino, '%'))
            order by t.fechaSolicitud desc
            """)
    List<Taxista> buscarPorTermino(String termino);

    Page<Taxista> findByEstadoSolicitud(
            EstadoSolicitudTaxista estadoSolicitud,
            Pageable pageable
    );

    Page<Taxista> findByDisponible(Boolean disponible, Pageable pageable);

    @Query("""
            select t
            from Taxista t
            where lower(t.numeroDocumento) like lower(concat('%', :termino, '%'))
               or lower(t.placaVehiculo) like lower(concat('%', :termino, '%'))
               or lower(t.usuario.nombres) like lower(concat('%', :termino, '%'))
               or lower(t.usuario.apellidos) like lower(concat('%', :termino, '%'))
            """)
    Page<Taxista> buscarPorTermino(String termino, Pageable pageable);

    @Query("""
            select t
            from Taxista t
            where t.estadoSolicitud = :estado
              and (
                    lower(t.numeroDocumento) like lower(concat('%', :termino, '%'))
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
