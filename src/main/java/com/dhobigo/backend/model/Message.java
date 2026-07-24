package com.dhobigo.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Message() {
    }

    private Message(Builder b) {
        this.id = b.id;
        this.order = b.order;
        this.sender = b.sender;
        this.content = b.content;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public User getSender() { return sender; }
    public String getContent() { return content; }
    public Instant getCreatedAt() { return createdAt; }

    public static class Builder {
        private Long id;
        private Order order;
        private User sender;
        private String content;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder order(Order order) { this.order = order; return this; }
        public Builder sender(User sender) { this.sender = sender; return this; }
        public Builder content(String content) { this.content = content; return this; }

        public Message build() { return new Message(this); }
    }
}
