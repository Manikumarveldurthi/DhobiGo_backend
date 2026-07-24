package com.dhobigo.backend.service;

import com.dhobigo.backend.exception.ApiException;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.model.WalletTransaction;
import com.dhobigo.backend.model.WalletTransactionType;
import com.dhobigo.backend.repository.UserRepository;
import com.dhobigo.backend.repository.WalletTransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Simple wallet: the running balance is denormalized onto User.walletBalance
 * for fast reads (profile/navbar), and every change also writes a
 * WalletTransaction row so the customer/admin can see history.
 */
@Service
public class WalletService {

    private final UserRepository userRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletService(UserRepository userRepository, WalletTransactionRepository walletTransactionRepository) {
        this.userRepository = userRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional
    public void credit(User user, int amount, WalletTransactionType type, String description) {
        if (amount <= 0) return;
        user.setWalletBalance(user.getWalletBalance() + amount);
        userRepository.save(user);
        walletTransactionRepository.save(WalletTransaction.builder()
                .user(user).amount(amount).type(type).description(description).build());
    }

    /** Debits up to `amount` — throws if the wallet doesn't have enough. */
    @Transactional
    public void debit(User user, int amount, WalletTransactionType type, String description) {
        if (amount <= 0) return;
        if (user.getWalletBalance() < amount) {
            throw new ApiException("Insufficient wallet balance", HttpStatus.BAD_REQUEST);
        }
        user.setWalletBalance(user.getWalletBalance() - amount);
        userRepository.save(user);
        walletTransactionRepository.save(WalletTransaction.builder()
                .user(user).amount(-amount).type(type).description(description).build());
    }

    /**
     * Applies as much of the wallet as possible (capped at both the wallet
     * balance and the order total) and returns how much was actually used.
     * Never throws — checkout should never fail just because the wallet
     * couldn't cover the whole order.
     */
    @Transactional
    public int applyToOrder(User user, int orderTotal, String orderCode) {
        int amount = Math.min(user.getWalletBalance(), orderTotal);
        if (amount <= 0) return 0;
        debit(user, amount, WalletTransactionType.ORDER_REDEMPTION, "Applied to order #" + orderCode);
        return amount;
    }

    public java.util.List<WalletTransaction> getHistory(Long userId) {
        return walletTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
