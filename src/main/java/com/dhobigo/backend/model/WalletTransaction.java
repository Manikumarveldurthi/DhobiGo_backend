package com.dhobigo.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

/** One line of wallet history (credit or debit) — the user's running balance itself lives on User.walletBalance. */
@Entity
@Table(name = "wallet_transaction")
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Positive = credit (added to wallet), negative = debit (spent from wallet). */
    @Column(nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletTransactionType type;

    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public WalletTransaction() {
    }

    private WalletTransaction(Builder b) {
        this.id = b.id;
        this.user = b.user;
        this.amount = b.amount;
        this.type = b.type;
        this.description = b.description;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }

    public WalletTransactionType getType() { return type; }
    public void setType(WalletTransactionType type) { this.type = type; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getCreatedAt() { return createdAt; }

    public static class Builder {
        private Long id;
        private User user;
        private Integer amount;
        private WalletTransactionType type;
        private String description;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder amount(Integer amount) { this.amount = amount; return this; }
        public Builder type(WalletTransactionType type) { this.type = type; return this; }
        public Builder description(String description) { this.description = description; return this; }

        public WalletTransaction build() { return new WalletTransaction(this); }
    }
}
