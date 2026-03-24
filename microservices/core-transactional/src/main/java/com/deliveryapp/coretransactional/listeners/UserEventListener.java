package com.deliveryapp.coretransactional.listeners;

import com.deliveryapp.coretransactional.dtos.request.Wallets.CreateWalletRequest;
import com.deliveryapp.coretransactional.events.UserCreatedEvent;
import com.deliveryapp.coretransactional.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEventListener {

    private final WalletService walletService;

    @RabbitListener(queues = "wallet.creation.queue")
    public void handleUserCreated(UserCreatedEvent event) {
        System.out.println("Recibido evento para crear billetera del usuario: " + event.getUserId());

        // 1. Usamos tu DTO exacto (CreateWalletRequest)
        CreateWalletRequest request = new CreateWalletRequest();

        // 2. Usamos el setDriverId de tu DTO, asignándole el ID que viene en el evento
        request.setDriverId(event.getUserId());

        // 3. Llamamos al servicio para crear la billetera
        walletService.createWallet(request);

        System.out.println("Billetera creada exitosamente para: " + event.getUserId());
    }
}