package com.dhobigo.backend.controller;

import com.dhobigo.backend.dto.WalletDtos.WalletResponse;
import com.dhobigo.backend.dto.WalletDtos.WalletTransactionResponse;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.model.WalletTransaction;
import com.dhobigo.backend.service.WalletService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Wallet balance + referral code + transaction history — any logged-in user. */
@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public WalletResponse me(@AuthenticationPrincipal User user) {
        List<WalletTransactionResponse> history = walletService.getHistory(user.getId()).stream()
                .map(this::toResponse)
                .toList();
        return new WalletResponse(user.getWalletBalance(), user.getReferralCode(), history);
    }

    private WalletTransactionResponse toResponse(WalletTransaction t) {
        return new WalletTransactionResponse(t.getId(), t.getAmount(), t.getType(), t.getDescription(), t.getCreatedAt());
    }
}
