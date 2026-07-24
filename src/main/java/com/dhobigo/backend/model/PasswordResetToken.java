package com.dhobigo.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    public PasswordResetToken() {
    }

    private PasswordResetToken(Builder b) {
        this.id = b.id;
        this.user = b.user;
        this.token = b.token;
        this.expiresAt = b.expiresAt;
        this.used = b.used;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public boolean isValid() {
        return !used && expiresAt.isAfter(Instant.now());
    }

    public static class Builder {
        private Long id;
        private User user;
        private String token;
        private Instant expiresAt;
        private boolean used = false;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder token(String token) { this.token = token; return this; }
        public Builder expiresAt(Instant expiresAt) { this.expiresAt = expiresAt; return this; }
        public Builder used(boolean used) { this.used = used; return this; }

        public PasswordResetToken build() { return new PasswordResetToken(this); }
    }
}
