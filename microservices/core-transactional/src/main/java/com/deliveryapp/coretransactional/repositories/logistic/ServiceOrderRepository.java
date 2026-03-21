package com.deliveryapp.coretransactional.repositories.logistic;

import com.deliveryapp.coretransactional.models.logistic.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

public interface ServiceOrderRepository extends  JpaRepository<ServiceOrder, UUID> {
    //Aqui despues ira logica con PosGis  para buscar pedidos por cercania
    Page<ServiceOrder> findByClientId(UUID clientId, Pageable pageable);
    Page<ServiceOrder> findByDriverId(UUID driverId, Pageable pageable);
    Page<ServiceOrder> findByMerchantId(UUID merchantId, Pageable pageable);
    Page<ServiceOrder> findByType(String type, Pageable pageable);

}
