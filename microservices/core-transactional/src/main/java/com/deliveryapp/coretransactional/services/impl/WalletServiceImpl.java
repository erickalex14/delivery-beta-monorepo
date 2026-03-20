package com.deliveryapp.coretransactional.services.impl;

import com.deliveryapp.coretransactional.dtos.request.Wallets.CreateWalletRequest;
import com.deliveryapp.coretransactional.dtos.request.Wallets.DebitWalletRequest;
import com.deliveryapp.coretransactional.dtos.request.Wallets.TopUpWalletRequest;
import com.deliveryapp.coretransactional.dtos.response.Wallets.WalletBalanceResponse;
import com.deliveryapp.coretransactional.models.Wallet;
import com.deliveryapp.coretransactional.repositories.WalletRepository;
import com.deliveryapp.coretransactional.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service // Le dice a Spring que este es el Chef Oficial
@RequiredArgsConstructor // Lombok inyecta el Repository automáticamente sin usar 'Autowired'
public class WalletServiceImpl implements WalletService {

    // Traemos al Bodeguero (Repository) para buscar datos
    private final WalletRepository walletRepository;

    @Override //Implementamos el método definido en la interfaz WalletService
    @Transactional //Si falla algo, hace Rollback automático

    // Este método es el encargado de recargar la billetera del conductor. Sigue los pasos de buscar la billetera, sumar el saldo, actualizar y guardar los cambios, y finalmente responder con el nuevo balance.
    public WalletBalanceResponse topUpWallet(UUID driverId, TopUpWalletRequest request) {

        // 1. BUSCAR: Le pedimos al bodeguero que busque la billetera usando el driverId que nos pasaron por parámetro
        Wallet wallet = walletRepository.findByUserId(driverId)
                .orElseThrow(() -> new RuntimeException("¡Error! Billetera no encontrada para este conductor"));

        // 2. LÓGICA DE NEGOCIO: Sumamos el saldo.
        // OJO: En Java BigDecimal no usa el símbolo '+', usa el método '.add()'
        BigDecimal newBalance = wallet.getBalance().add(request.getAmount());

        // 3. ACTUALIZAR: Usamos el Setter manual que dejamos en la entidad
        wallet.setBalance(newBalance);

        // 4. GUARDAR: Le decimos al bodeguero que guarde los cambios en PostgreSQL
        walletRepository.save(wallet);

        // 5. RESPONDER: Construimos el DTO de salida sin exponer datos sensibles
        return WalletBalanceResponse.builder()
                .driverId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }


    //Creacion de una billetera para un nuevo conductor (Ojo aqui es donde pel microservicio de node hara la llamada para crear la billetera cuando un usuario se rregistre en su logica)
    @Override
    @Transactional
    public WalletBalanceResponse createWallet(CreateWalletRequest request){
        //Revisar si ya hay una billetera para este usuario
        if(walletRepository.findByUserId(request.getDriverId()).isPresent()){
            throw new RuntimeException("¡Error! Ya existe una billetera para este conductor");
        }

        //Crear una nueva billetera con el balance inicial
        Wallet newWallet = new Wallet();
        newWallet.setUserId(request.getDriverId());
        //No seteamos el balance porque ya lo inicaliza en 0 y el currency por defecto es USD
        //Guardar la nueva billetera en la base de datos
        walletRepository.save(newWallet);

        //Responder con el balance inicial de la nueva billetera
        return WalletBalanceResponse.builder()
                .driverId(newWallet.getUserId())
                .balance(newWallet.getBalance())
                .currency(newWallet.getCurrency())
                .build();
    }

    //Metodo para debitar saldo de la billetera
    @Override
    @Transactional
    public WalletBalanceResponse debitWallet(UUID driverId, DebitWalletRequest request) {
        //Validad que existe la billetera
        Wallet wallet = walletRepository.findByUserId(driverId)
                .orElseThrow(() -> new RuntimeException("¡Error! No se encontro la billetera"));
        //Validar que el saldo sea suficiente para el débito
        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("¡Error! Saldo insuficiente para esta operación");
        }

        //Realizar el débito restando el monto al balance actual
        BigDecimal newBalance = wallet.getBalance().subtract(request.getAmount());
        wallet.setBalance(newBalance);

        //Guardar los cambios en la base de datos
        walletRepository.save(wallet);

        //Responder con el nuevo balance después del débito
        return WalletBalanceResponse.builder()
                .driverId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }
}