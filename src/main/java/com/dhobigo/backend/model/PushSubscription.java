package com.dhobigo.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * A browser's Web Push subscription (from the PushManager API), so the
 * backend can send a notification even when the tab is closed. Storage-only
 * for now — see PushNotificationService for why actually sending is a stub
 * until you plug in VAPID keys + a web-push library of your choice.
 */
@Entity
@Table(name = "push_subscription")
public class PushSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 512, unique = true)
    private String endpoint;

    @Column(nullable = false)
    private String p256dh;

    @Column(nullable = false)
    private String auth;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public PushSubscription() {
    }

    private PushSubscription(Builder b) {
        this.id = b.id;
        this.user = b.user;
        this.endpoint = b.endpoint;
        this.p256dh = b.p256dh;
        this.auth = b.auth;
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

    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getP256dh() { return p256dh; }
    public void setP256dh(String p256dh) { this.p256dh = p256dh; }

    public String getAuth() { return auth; }
    public void setAuth(String auth) { this.auth = auth; }

    public Instant getCreatedAt() { return createdAt; }

    public static class Builder {
        private Long id;
        private User user;
        private String endpoint;
        private String p256dh;
        private String auth;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder endpoint(String endpoint) { this.endpoint = endpoint; return this; }
        public Builder p256dh(String p256dh) { this.p256dh = p256dh; return this; }
        public Builder auth(String auth) { this.auth = auth; return this; }

        public PushSubscription build() { return new PushSubscription(this); }
    }
}
