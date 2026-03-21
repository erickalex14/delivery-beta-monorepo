package com.deliveryapp.coretransactional.repositories.logistic;

import com.deliveryapp.coretransactional.models.logistic.ServiceOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ServiceOrderRepository extends  JpaRepository<ServiceOrder, UUID> {
    //Aqui despues ira logica con PosGis  para buscar pedidos por cercania
    List<ServiceOrder> findByClientId(UUID clientId);

}
