package com.dhobigo.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "order_item")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** Snapshot fields (not a live FK to CatalogItem) — so historical orders
        stay accurate even if catalog prices change later. */
    @Column(nullable = false)
    private String itemKey;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType service;

    @Column(nullable = false)
    private Integer price;

    @Column(nullable = false)
    private Integer qty;

    /** Optional per-item note from the customer, e.g. "wine stain on collar". */
    @Column(columnDefinition = "TEXT")
    private String specialInstructions;

    /** Optional customer-attached photo (e.g. of a stain) — data URL or link. */
    @Column(columnDefinition = "LONGTEXT")
    private String photoUrl;

    public OrderItem() {
    }

    private OrderItem(Builder b) {
        this.id = b.id;
        this.order = b.order;
        this.itemKey = b.itemKey;
        this.name = b.name;
        this.service = b.service;
        this.price = b.price;
        this.qty = b.qty;
        this.specialInstructions = b.specialInstructions;
        this.photoUrl = b.photoUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }

    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ServiceType getService() { return service; }
    public void setService(ServiceType service) { this.service = service; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    public Integer getQty() { return qty; }
    public void setQty(Integer qty) { this.qty = qty; }

    public String getSpecialInstructions() { return specialInstructions; }
    public void setSpecialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public static class Builder {
        private Long id;
        private Order order;
        private String itemKey;
        private String name;
        private ServiceType service;
        private Integer price;
        private Integer qty;
        private String specialInstructions;
        private String photoUrl;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder order(Order order) { this.order = order; return this; }
        public Builder itemKey(String itemKey) { this.itemKey = itemKey; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder service(ServiceType service) { this.service = service; return this; }
        public Builder price(Integer price) { this.price = price; return this; }
        public Builder qty(Integer qty) { this.qty = qty; return this; }
        public Builder specialInstructions(String specialInstructions) { this.specialInstructions = specialInstructions; return this; }
        public Builder photoUrl(String photoUrl) { this.photoUrl = photoUrl; return this; }

        public OrderItem build() { return new OrderItem(this); }
    }
}
