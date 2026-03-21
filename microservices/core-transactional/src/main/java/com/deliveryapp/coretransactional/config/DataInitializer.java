package com.deliveryapp.coretransactional.config;

import com.deliveryapp.coretransactional.models.logistic.OrderStatus;
import com.deliveryapp.coretransactional.repositories.logistic.OrderStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final OrderStatusRepository orderStatusRepository;

    @Override
    public void run(String... args) {
        if (orderStatusRepository.count() == 0) {
            saveStatus("CREATED", "RIDE", true, false);
            saveStatus("ACCEPTED", "RIDE", false, false);
            saveStatus("DELIVERED", "RIDE", false, true);
            saveStatus("CANCELLED", "RIDE", false, true);

            // --- ESTADOS PARA DELIVERY (Pedidos/Comida) ---
            saveStatus("CREATED", "DELIVERY", true, false);
            saveStatus("ACCEPTED", "DELIVERY", false, false);
            saveStatus("PREPARING", "DELIVERY", false, false);
            saveStatus("OUT_FOR_DELIVERY", "DELIVERY", false, false);
            saveStatus("DELIVERED", "DELIVERY", false, true);
            saveStatus("CANCELLED", "DELIVERY", false, true);

            System.out.println("Estados de logística inicializados correctamente.");
        }
    }

    private void saveStatus(String code, String type, boolean initial, boolean isFinal) {
        OrderStatus status = new OrderStatus();
        status.setCode(code);
        status.setType(type);
        status.setInitial(initial);
        status.setFinal(isFinal);
        orderStatusRepository.save(status);
    }
}
