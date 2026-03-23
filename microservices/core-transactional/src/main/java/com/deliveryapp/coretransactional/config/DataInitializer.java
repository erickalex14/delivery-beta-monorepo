package com.deliveryapp.coretransactional.config;

import com.deliveryapp.coretransactional.models.logistic.OrderStatus;
import com.deliveryapp.coretransactional.models.logistic.OrderStatusTransition;
import com.deliveryapp.coretransactional.repositories.logistic.OrderStatusRepository;
import com.deliveryapp.coretransactional.repositories.logistic.OrderStatusTransitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final OrderStatusRepository orderStatusRepository;

    // Inyectamos el repositorio de transiciones
    private final OrderStatusTransitionRepository transitionRepository;

    @Override
    public void run(String... args) {

        // Solo sembramos si la base de datos está vacía
        if (orderStatusRepository.count() == 0) {

            // --- 1. CREAR Y GUARDAR ESTADOS PARA RIDE (Fletes) ---
            OrderStatus rideCreated = saveStatus("CREATED", "RIDE", true, false);
            OrderStatus rideAccepted = saveStatus("ACCEPTED", "RIDE", false, false);
            OrderStatus rideDelivered = saveStatus("DELIVERED", "RIDE", false, true);
            OrderStatus rideCancelled = saveStatus("CANCELLED", "RIDE", false, true);

            // --- 2. CREAR Y GUARDAR ESTADOS PARA DELIVERY (Comida) ---
            OrderStatus delCreated = saveStatus("CREATED", "DELIVERY", true, false);
            OrderStatus delAccepted = saveStatus("ACCEPTED", "DELIVERY", false, false);
            OrderStatus delPreparing = saveStatus("PREPARING", "DELIVERY", false, false);
            OrderStatus delOut = saveStatus("OUT_FOR_DELIVERY", "DELIVERY", false, false);
            OrderStatus delDelivered = saveStatus("DELIVERED", "DELIVERY", false, true);
            OrderStatus delCancelled = saveStatus("CANCELLED", "DELIVERY", false, true);

            System.out.println("Estados de logística inicializados correctamente.");

            // --- 3. SEMBRAR LAS REGLAS DE TRANSICIÓN
            if (transitionRepository.count() == 0) {

                // Reglas para RIDE (Fletes)
                // El conductor acepta la carrera (No pide PIN)
                saveTransition(rideCreated, rideAccepted, "DRIVER", false);
                // El conductor finaliza la carrera (SÍ pide PIN del cliente)
                saveTransition(rideAccepted, rideDelivered, "DRIVER", true);
                // El cliente puede cancelar antes de que se acepte
                saveTransition(rideCreated, rideCancelled, "CLIENT", false);

                // Reglas para DELIVERY (Comida/Paquetes)
                // El comercio acepta el pedido
                saveTransition(delCreated, delAccepted, "MERCHANT", false);
                // El comercio empieza a preparar
                saveTransition(delAccepted, delPreparing, "MERCHANT", false);
                // El conductor recoge la comida y va en camino
                saveTransition(delPreparing, delOut, "DRIVER", false);
                // El conductor entrega la comida (SÍ pide PIN del cliente)
                saveTransition(delOut, delDelivered, "DRIVER", true);

                // Cancelaciones permitidas en Delivery
                saveTransition(delCreated, delCancelled, "CLIENT", false);
                saveTransition(delCreated, delCancelled, "MERCHANT", false);

                System.out.println("Reglas de transición (Máquina de Estados) inicializadas correctamente.");
            }
        }
    }

    // Modificamos este método para que nos devuelva el objeto guardado y poder usarlo en las reglas
    private OrderStatus saveStatus(String code, String type, boolean initial, boolean isFinal) {
        OrderStatus status = new OrderStatus();
        status.setCode(code);
        status.setType(type);
        status.setInitial(initial);
        status.setFinal(isFinal);
        return orderStatusRepository.save(status);
    }

    // Método helper para crear las reglas de salto
    private void saveTransition(OrderStatus from, OrderStatus to, String role, boolean requiresPin) {
        OrderStatusTransition transition = new OrderStatusTransition();
        transition.setFromStatus(from);
        transition.setToStatus(to);
        transition.setAllowedRole(role);
        transition.setRequiresPin(requiresPin);
        transitionRepository.save(transition);
    }
}