package com.deliveryapp.coretransactional.repositories.finance;

import com.deliveryapp.coretransactional.models.finance.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    //Auditoría Cruzada: Buscar la entrada contable usando el ID del pedido/flete
    Optional<LedgerEntry> findByReferenceId(UUID referenceId);

    //Filtro de Auditoría: Buscar por tipo (Ej: Tráeme todas las recargas "TOP_UP" de hoy)
    List<LedgerEntry> findByReferenceTypeOrderByCreatedAtDesc(String referenceType);
}
