package com.iotelito.taxiweb.repository;

import com.iotelito.taxiweb.model.DocumentoTaxista;
import com.iotelito.taxiweb.model.Taxista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoTaxistaRepository extends JpaRepository<DocumentoTaxista, Long> {

    List<DocumentoTaxista> findByTaxistaOrderByTipo(Taxista taxista);

    Optional<DocumentoTaxista> findByIdAndTaxistaId(Long id, Long taxistaId);
}
