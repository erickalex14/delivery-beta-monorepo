package com.deliveryapp.coretransactional.services.impl;

import com.deliveryapp.coretransactional.dtos.request.Wallets.CreateWalletRequest;
import com.deliveryapp.coretransactional.dtos.request.Wallets.DebitWalletRequest;
import com.deliveryapp.coretransactional.dtos.request.Wallets.TopUpWalletRequest;
import com.deliveryapp.coretransactional.dtos.response.Wallets.WalletBalanceResponse;
import com.deliveryapp.coretransactional.dtos.response.Wallets.WalletMovementResponse;
import com.deliveryapp.coretransactional.models.LedgerEntry;
import com.deliveryapp.coretransactional.models.LedgerPosting;
import com.deliveryapp.coretransactional.models.Wallet;
import com.deliveryapp.coretransactional.repositories.LedgerEntryRepository;
import com.deliveryapp.coretransactional.repositories.LedgerPostingRepository;
import com.deliveryapp.coretransactional.repositories.WalletRepository;
import com.deliveryapp.coretransactional.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service // Le dice a Spring que este es el Chef Oficial
@RequiredArgsConstructor // Lombok inyecta el Repository automáticamente sin usar 'Autowired'
public class WalletServiceImpl implements WalletService {

    // Traemos al Bodeguero (Repository) para buscar datos
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository; // Para registrar cada movimiento en el libro contable
    private final LedgerPostingRepository ledgerPostingRepository;
    // Para registrar cada asiento contable relacionado con las transacciones de la billetera


    // Este método es el encargado de recargar la billetera del conductor. Sigue los pasos de buscar la billetera, sumar el saldo, actualizar y guardar los cambios, y finalmente responder con el nuevo balance.
    @Override //Implementamos el método definido en la interfaz WalletService
    @Transactional //Si falla algo, hace Rollback automático
    public WalletBalanceResponse topUpWallet(UUID driverId, TopUpWalletRequest request) {
        // buscar la billetera del conductor por su ID
        Wallet wallet = walletRepository.findByUserId(driverId)
                .orElseThrow(() -> new RuntimeException("¡Error! No se encontro la billetera"));

        //crear el recibo (LedgerEntry) para esta recarga
        LedgerEntry entry = new LedgerEntry();
        entry.setReferenceType("TOP_UP");
        entry.setDescription("Recarga de saldo a la billetera");
        //entry.setReferenceId(Aqui iria el id de la ordea, talvez id de la transferencia o algo asi, pero por ahora lo dejamos null porque no tenemos esa logica implementada);
        ledgerEntryRepository.save(entry);

        //crear el asiento contable (LedgerPosting) para esta recarga
        LedgerPosting posting = new LedgerPosting();
        posting.setLedgerEntry(entry);
        posting.setWallet(wallet);
        posting.setAmount(request.getAmount());
        posting.setDirection("CREDIT"); // Es una recarga, así que es un crédito
        ledgerPostingRepository.save(posting);

        // sumar el monto de la recarga al balance actual de la billetera (cache)
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        // responder con el nuevo balance de la billetera
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
        //Validar que la billetera exista
        Wallet wallet = walletRepository.findByUserId(driverId)
                .orElseThrow(() -> new RuntimeException("¡Error! No se encontro la billetera"));

        //Validar que haya suficiente saldo para debitar
        if(wallet.getBalance().compareTo(request.getAmount()) < 0){
            throw new RuntimeException("¡Error! Saldo insuficiente en la billetera");
        }

        //crear el recibo (LedgerEntry) para esta transacción
        LedgerEntry entry = new LedgerEntry();
        entry.setReferenceType("ORDER_COMISSION");
        entry.setDescription("Comisión por servicio realizado");
        //entry.setReferenceId(Aqui iria el id de la orden o el viaje cuando se implemente esa logica);
        ledgerEntryRepository.save(entry);

        //crear el asiento contable (LedgerPosting) para esta transacción
        LedgerPosting posting = new LedgerPosting();
        posting.setLedgerEntry(entry);
        posting.setWallet(wallet);
        posting.setAmount(request.getAmount());
        posting.setDirection("DEBIT"); // Es un débito porque se está descontando dinero
        ledgerPostingRepository.save(posting);

        // restar el monto del débito al balance actual de la billetera (cache)
        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);

        // responder con el nuevo balance de la billetera
        return WalletBalanceResponse.builder()
                .driverId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }


    //Obtener el historial de movimientos de la billetera, mostrando la descripción, monto, dirección (crédito o débito) y fecha de cada movimiento. Se ordena por fecha descendente para mostrar primero los movimientos más recientes.
    @Override
    @Transactional(readOnly = true)
    public List<WalletMovementResponse> getMovementHistory(UUID driverId) {
        //Validar que la billetera exista
        Wallet wallet = walletRepository.findByUserId(driverId)
                .orElseThrow(() -> new RuntimeException("¡Error! No se encontro la billetera"));
        //Obtener los movimientos (LedgerPostings) relacionados con esta billetera
        return ledgerPostingRepository.findByWalletUserIdOrderByCreatedAtDesc(driverId)
                .stream()
                .map(posting -> WalletMovementResponse.builder()
                        .description(posting.getLedgerEntry().getDescription())
                        .amount(posting.getAmount())
                        .direction(posting.getDirection())
                        .date(posting.getCreatedAt())
                        .build())
                .toList();
    }


}