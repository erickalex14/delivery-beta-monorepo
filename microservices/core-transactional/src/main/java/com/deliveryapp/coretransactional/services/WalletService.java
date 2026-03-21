package com.deliveryapp.coretransactional.services;

import com.deliveryapp.coretransactional.dtos.request.Wallets.TopUpWalletRequest;
import com.deliveryapp.coretransactional.dtos.response.Wallets.WalletBalanceResponse;
import com.deliveryapp.coretransactional.dtos.request.Wallets.CreateWalletRequest;
import com.deliveryapp.coretransactional.dtos.request.Wallets.DebitWalletRequest;
import com.deliveryapp.coretransactional.dtos.response.Wallets.WalletMovementResponse;
import com.deliveryapp.coretransactional.models.finance.Wallet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface WalletService {
    // Le pasamos el ID explícitamente
    WalletBalanceResponse topUpWallet(UUID driverId, TopUpWalletRequest request);

    WalletBalanceResponse createWallet(CreateWalletRequest request);

    WalletBalanceResponse debitWallet(UUID driverId, DebitWalletRequest request);

    Page<WalletMovementResponse> getMovementHistory(UUID driverId, Pageable pageable);

    Page<Wallet> getWalletsByTenantId(UUID tenantId, Pageable pageable);

}