package com.dhobigo.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "review", uniqueConstraints = @UniqueConstraint(columnNames = "order_id"))
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dhobi_id", nullable = false)
    private User dhobi;

    @Column(nullable = false)
    private Integer rating; // 1-5

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Review() {
    }

    private Review(Builder b) {
        this.id = b.id;
        this.order = b.order;
        this.customer = b.customer;
        this.dhobi = b.dhobi;
        this.rating = b.rating;
        this.comment = b.comment;
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
    public User getCustomer() { return customer; }
    public User getDhobi() { return dhobi; }
    public Integer getRating() { return rating; }
    public String getComment() { return comment; }
    public Instant getCreatedAt() { return createdAt; }

    public static class Builder {
        private Long id;
        private Order order;
        private User customer;
        private User dhobi;
        private Integer rating;
        private String comment;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder order(Order order) { this.order = order; return this; }
        public Builder customer(User customer) { this.customer = customer; return this; }
        public Builder dhobi(User dhobi) { this.dhobi = dhobi; return this; }
        public Builder rating(Integer rating) { this.rating = rating; return this; }
        public Builder comment(String comment) { this.comment = comment; return this; }

        public Review build() { return new Review(this); }
    }
}
