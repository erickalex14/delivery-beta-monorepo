package com.deliveryapp.coretransactional.services.impl;

import com.deliveryapp.coretransactional.dtos.request.Wallets.CreateWalletRequest;
import com.deliveryapp.coretransactional.dtos.request.Wallets.DebitWalletRequest;
import com.deliveryapp.coretransactional.dtos.request.Wallets.TopUpWalletRequest;
import com.deliveryapp.coretransactional.dtos.response.Wallets.WalletBalanceResponse;
import com.deliveryapp.coretransactional.dtos.response.Wallets.WalletMovementResponse;
import com.deliveryapp.coretransactional.models.finance.LedgerEntry;
import com.deliveryapp.coretransactional.models.finance.LedgerPosting;
import com.deliveryapp.coretransactional.models.finance.Wallet;
import com.deliveryapp.coretransactional.repositories.finance.LedgerEntryRepository;
import com.deliveryapp.coretransactional.repositories.finance.LedgerPostingRepository;
import com.deliveryapp.coretransactional.repositories.finance.WalletRepository;
import com.deliveryapp.coretransactional.services.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final LedgerPostingRepository ledgerPostingRepository;

    //Recargar Billetera
    @Override
    @Transactional
    public WalletBalanceResponse topUpWallet(UUID driverId, TopUpWalletRequest request) {
        // RECARGA SEGURA EN BASE DE DATOS
        int rowsUpdated = walletRepository.addBalanceSafely(driverId, request.getAmount());
        if (rowsUpdated == 0) throw new RuntimeException("No se encontró la billetera para recargar");

        // Buscamos la billetera YA actualizada
        Wallet wallet = walletRepository.findByUserId(driverId).orElseThrow();

        //Entrada al ledger entry
        LedgerEntry entry = new LedgerEntry();
        entry.setReferenceType("TOP_UP");
        entry.setDescription("Recarga de saldo a la billetera: " + request.getDescription());
        ledgerEntryRepository.save(entry);

        //Asiento contable
        LedgerPosting posting = new LedgerPosting();
        posting.setLedgerEntry(entry);
        posting.setWallet(wallet);
        posting.setAmount(request.getAmount());
        posting.setDirection("CREDIT");
        ledgerPostingRepository.save(posting);

        return WalletBalanceResponse.builder()
                .driverId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }

    //Crear billetera (Evento desencadenado al crear usuario en el Identity-service, osea
    //Identityservice manda un mensaje con rabitMQ que se recibe aqui y dispara el metodo de crear billetera)
    @Override
    @Transactional
    public WalletBalanceResponse createWallet(CreateWalletRequest request){
        // IDEMPOTENCIA PARA RABBITMQ
        Optional<Wallet> existingWallet = walletRepository.findByUserId(request.getDriverId());
        if(existingWallet.isPresent()){
            Wallet wallet = existingWallet.get();
            return WalletBalanceResponse.builder()
                    .driverId(wallet.getUserId())
                    .balance(wallet.getBalance())
                    .currency(wallet.getCurrency())
                    .build();
        }

        Wallet newWallet = new Wallet();
        newWallet.setUserId(request.getDriverId());
        newWallet.setBalance(BigDecimal.ZERO);
        newWallet.setCurrency("USD");
        walletRepository.save(newWallet);

        return WalletBalanceResponse.builder()
                .driverId(newWallet.getUserId())
                .balance(newWallet.getBalance())
                .currency(newWallet.getCurrency())
                .build();
    }

    //Debitos en las billeteras
    @Override
    @Transactional
    public WalletBalanceResponse debitWallet(UUID driverId, DebitWalletRequest request) {
        // COBRO SEGURO EN BASE DE DATOS (Previene saldos negativos y Lost Updates)
        int rowsUpdated = walletRepository.deductBalanceSafely(driverId, request.getAmount());
        if(rowsUpdated == 0){
            throw new RuntimeException("¡Error! Saldo insuficiente en la billetera o cuenta no encontrada");
        }

        Wallet wallet = walletRepository.findByUserId(driverId).orElseThrow();

        LedgerEntry entry = new LedgerEntry();
        entry.setReferenceType("ORDER_COMISSION");
        entry.setDescription(request.getDescription());
        ledgerEntryRepository.save(entry);

        LedgerPosting posting = new LedgerPosting();
        posting.setLedgerEntry(entry);
        posting.setWallet(wallet);
        posting.setAmount(request.getAmount());
        posting.setDirection("DEBIT");
        ledgerPostingRepository.save(posting);

        return WalletBalanceResponse.builder()
                .driverId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }

    //Ver movimientos de la billetera
    @Override
    @Transactional(readOnly = true)
    public Page<WalletMovementResponse> getMovementHistory(UUID driverId, Pageable pageable) {
        Wallet wallet = walletRepository.findByUserId(driverId)
                .orElseThrow(() -> new RuntimeException("¡Error! No se encontro la billetera"));

        return ledgerPostingRepository.findByWalletUserIdOrderByCreatedAtDesc(driverId, pageable)
                .map(posting -> WalletMovementResponse.builder()
                        .description(posting.getLedgerEntry().getDescription())
                        .amount(posting.getAmount())
                        .direction(posting.getDirection())
                        .date(posting.getCreatedAt())
                        .build());
    }


    //Multi tenant
    @Override
    public Page<Wallet> getWalletsByTenantId(UUID tenantId, Pageable pageable) {
        return walletRepository.findByTenantId(tenantId, pageable);
    }
}