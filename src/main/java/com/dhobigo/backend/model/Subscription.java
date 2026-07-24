package com.dhobigo.backend.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A customer's saved recurring-pickup plan (e.g. "every Tuesday & Friday").
 * Deliberately does NOT auto-place a real order with items (items vary each
 * time and shouldn't be auto-charged without the customer looking) — instead
 * it tracks when the next pickup is due, so subscriptions.html can prompt
 * "today's pickup is due, tap to build your order", pre-filled with the
 * saved address/slot. See SubscriptionService for the full flow.
 */
@Entity
@Table(name = "subscription")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType service;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionFrequency frequency;

    @Column(nullable = false)
    private String pickupSlot;

    @Column(nullable = false)
    private String pickupAddress;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDate nextRunDate;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Subscription() {
    }

    private Subscription(Builder b) {
        this.id = b.id;
        this.customer = b.customer;
        this.service = b.service;
        this.frequency = b.frequency;
        this.pickupSlot = b.pickupSlot;
        this.pickupAddress = b.pickupAddress;
        this.active = b.active;
        this.nextRunDate = b.nextRunDate;
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

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public ServiceType getService() { return service; }
    public void setService(ServiceType service) { this.service = service; }

    public SubscriptionFrequency getFrequency() { return frequency; }
    public void setFrequency(SubscriptionFrequency frequency) { this.frequency = frequency; }

    public String getPickupSlot() { return pickupSlot; }
    public void setPickupSlot(String pickupSlot) { this.pickupSlot = pickupSlot; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDate getNextRunDate() { return nextRunDate; }
    public void setNextRunDate(LocalDate nextRunDate) { this.nextRunDate = nextRunDate; }

    public Instant getCreatedAt() { return createdAt; }

    public static class Builder {
        private Long id;
        private User customer;
        private ServiceType service;
        private SubscriptionFrequency frequency;
        private String pickupSlot;
        private String pickupAddress;
        private boolean active = true;
        private LocalDate nextRunDate;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder customer(User customer) { this.customer = customer; return this; }
        public Builder service(ServiceType service) { this.service = service; return this; }
        public Builder frequency(SubscriptionFrequency frequency) { this.frequency = frequency; return this; }
        public Builder pickupSlot(String pickupSlot) { this.pickupSlot = pickupSlot; return this; }
        public Builder pickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; return this; }
        public Builder active(boolean active) { this.active = active; return this; }
        public Builder nextRunDate(LocalDate nextRunDate) { this.nextRunDate = nextRunDate; return this; }

        public Subscription build() { return new Subscription(this); }
    }
}
