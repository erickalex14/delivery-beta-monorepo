package com.deliveryapp.coretransactional.services;

import com.deliveryapp.coretransactional.dtos.request.Wallets.TopUpWalletRequest;
import com.deliveryapp.coretransactional.dtos.response.Wallets.WalletBalanceResponse;
import com.deliveryapp.coretransactional.dtos.request.Wallets.CreateWalletRequest;
import com.deliveryapp.coretransactional.dtos.request.Wallets.DebitWalletRequest;

import java.util.UUID;

public interface WalletService {
    // Le pasamos el ID explícitamente
    WalletBalanceResponse topUpWallet(UUID driverId, TopUpWalletRequest request);

    WalletBalanceResponse createWallet(CreateWalletRequest request);

    WalletBalanceResponse debitWallet(UUID driverId, DebitWalletRequest request);
}