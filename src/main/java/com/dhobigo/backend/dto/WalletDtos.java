package com.dhobigo.backend.dto;

import com.dhobigo.backend.model.WalletTransactionType;

import java.time.Instant;
import java.util.List;

public class WalletDtos {

    public record WalletResponse(
            int balance,
            String referralCode,
            List<WalletTransactionResponse> history
    ) {}

    public record WalletTransactionResponse(
            Long id,
            int amount,
            WalletTransactionType type,
            String description,
            Instant createdAt
    ) {}
}
