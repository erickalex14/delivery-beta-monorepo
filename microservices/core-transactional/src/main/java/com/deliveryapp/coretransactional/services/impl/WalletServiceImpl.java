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

@Service // Le dice a Spring que este es el Chef Oficial
@RequiredArgsConstructor // Lombok inyecta el Repository automáticamente sin usar 'Autowired'
public class WalletServiceImpl implements WalletService {

    // Traemos al Bodeguero (Repository) para buscar datos
    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerEntryRepository; // Para registrar cada movimiento en el libro contable
    private final LedgerPostingRepository ledgerPostingRepository; // Para registrar cada asiento contable


    // Recarga la billetera del conductor
    @Override
    @Transactional
    public WalletBalanceResponse topUpWallet(UUID driverId, TopUpWalletRequest request) {
        Wallet wallet = walletRepository.findByUserId(driverId)
                .orElseThrow(() -> new RuntimeException("¡Error! No se encontro la billetera"));

        // Crear el recibo (LedgerEntry)
        LedgerEntry entry = new LedgerEntry();
        entry.setReferenceType("TOP_UP");
        entry.setDescription("Recarga de saldo a la billetera");
        ledgerEntryRepository.save(entry);

        // Crear el asiento contable (LedgerPosting)
        LedgerPosting posting = new LedgerPosting();
        posting.setLedgerEntry(entry);
        posting.setWallet(wallet);
        posting.setAmount(request.getAmount());
        posting.setDirection("CREDIT");
        ledgerPostingRepository.save(posting);

        // Sumar el monto al balance
        wallet.setBalance(wallet.getBalance().add(request.getAmount()));
        walletRepository.save(wallet);

        return WalletBalanceResponse.builder()
                .driverId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }


    // 🛡️ CREACIÓN IDEMPOTENTE DE BILLETERA (Preparada para RabbitMQ)
    @Override
    @Transactional
    public WalletBalanceResponse createWallet(CreateWalletRequest request) {

        Optional<Wallet> existingWallet = walletRepository.findByUserId(request.getDriverId());

        // CONTROL DE IDEMPOTENCIA: Si RabbitMQ manda el evento dos veces, no explotamos.
        // Simplemente interceptamos la falla y devolvemos la billetera que ya existe.
        if(existingWallet.isPresent()){
            System.out.println("Idempotencia activada: La billetera para el usuario " + request.getDriverId() + " ya existe. Ignorando evento duplicado.");
            Wallet wallet = existingWallet.get();
            return WalletBalanceResponse.builder()
                    .driverId(wallet.getUserId())
                    .balance(wallet.getBalance())
                    .currency(wallet.getCurrency())
                    .build();
        }

        // Crear una nueva billetera
        Wallet newWallet = new Wallet();
        newWallet.setUserId(request.getDriverId());
        newWallet.setBalance(BigDecimal.ZERO); // ⚠️ CRÍTICO: Inicializar explícitamente en ZERO
        newWallet.setCurrency("USD"); // ⚠️ CRÍTICO: Inicializar moneda explícitamente

        walletRepository.save(newWallet);

        return WalletBalanceResponse.builder()
                .driverId(newWallet.getUserId())
                .balance(newWallet.getBalance())
                .currency(newWallet.getCurrency())
                .build();
    }

    // Debitar saldo de la billetera
    @Override
    @Transactional
    public WalletBalanceResponse debitWallet(UUID driverId, DebitWalletRequest request) {
        Wallet wallet = walletRepository.findByUserId(driverId)
                .orElseThrow(() -> new RuntimeException("¡Error! No se encontro la billetera"));

        // Validar que haya suficiente saldo
        if(wallet.getBalance().compareTo(request.getAmount()) < 0){
            throw new RuntimeException("¡Error! Saldo insuficiente en la billetera");
        }

        LedgerEntry entry = new LedgerEntry();
        entry.setReferenceType("ORDER_COMISSION");
        entry.setDescription("Comisión por servicio realizado");
        ledgerEntryRepository.save(entry);

        LedgerPosting posting = new LedgerPosting();
        posting.setLedgerEntry(entry);
        posting.setWallet(wallet);
        posting.setAmount(request.getAmount());
        posting.setDirection("DEBIT");
        ledgerPostingRepository.save(posting);

        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        walletRepository.save(wallet);

        return WalletBalanceResponse.builder()
                .driverId(wallet.getUserId())
                .balance(wallet.getBalance())
                .currency(wallet.getCurrency())
                .build();
    }

    // Historial de movimientos
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

    // Auditoria Multi-tenant
    @Override
    public Page<Wallet> getWalletsByTenantId(UUID tenantId, Pageable pageable) {
        return walletRepository.findByTenantId(tenantId, pageable);
    }
}