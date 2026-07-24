package com.dhobigo.backend.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-facing code like "DG-48213", shown in the UI instead of the raw id */
    @Column(nullable = false, unique = true)
    private String orderCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dhobi_id")
    private User dhobi; // nullable until assigned; role must be DHOBI

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(nullable = false)
    private String pickupAddress;

    // Optional — populated when the customer used "Use current location" at
    // checkout (payment.js) instead of typing an address. Nullable because
    // manually-typed addresses don't carry coordinates. Used to auto-assign
    // the nearest available dhobi (see OrderService#resolveDhobi).
    private Double pickupLatitude;
    private Double pickupLongitude;

    @Column(nullable = false)
    private String pickupSlot;

    @Column(nullable = false)
    private String paymentMethod; // "upi" | "card" | "cod"

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // Only set for upi/card orders once Razorpay is actually configured —
    // null for COD and for demo-mode orders placed before Razorpay was set up.
    private String razorpayOrderId;
    private String razorpayPaymentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStage stage = OrderStage.PLACED;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AcceptanceStatus acceptanceStatus = AcceptanceStatus.PENDING;

    // Dhobis who've already declined this order — excluded when the
    // customer picks a replacement, so a decline can't loop back to the
    // same dhobi. Separate table since a single order can accumulate
    // several declines before the customer finds someone available.
    @ElementCollection
    @CollectionTable(name = "order_declined_dhobis", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "dhobi_id")
    private java.util.Set<Long> declinedByDhobiIds = new java.util.HashSet<>();

    // ===== Added: photo proof, garment-level sub-stage, wallet redemption
    // (additive — all nullable, existing orders simply don't have them) =====

    /** Dhobi-uploaded photo of the items at pickup — trust/dispute-resolution. */
    @Column(columnDefinition = "LONGTEXT")
    private String pickupPhotoUrl;

    /** Dhobi-uploaded photo of the finished items before delivery. */
    @Column(columnDefinition = "LONGTEXT")
    private String deliveryPhotoUrl;

    /** Garment-level status, only meaningful while stage = WASHING. */
    @Enumerated(EnumType.STRING)
    private WashSubStage subStage;

    /** How many rupees of the customer's wallet were applied to this order. */
    private Integer walletAmountUsed;

    @Column(nullable = false)
    private Integer subtotal;

    @Column(nullable = false)
    private Integer deliveryFee;

    @Column(nullable = false)
    private Integer total;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant updatedAt;

    public Order() {
    }

    private Order(Builder b) {
        this.id = b.id;
        this.orderCode = b.orderCode;
        this.customer = b.customer;
        this.dhobi = b.dhobi;
        this.items = b.items;
        this.pickupAddress = b.pickupAddress;
        this.pickupLatitude = b.pickupLatitude;
        this.pickupLongitude = b.pickupLongitude;
        this.pickupSlot = b.pickupSlot;
        this.paymentMethod = b.paymentMethod;
        this.paymentStatus = b.paymentStatus;
        this.razorpayOrderId = b.razorpayOrderId;
        this.razorpayPaymentId = b.razorpayPaymentId;
        this.stage = b.stage;
        this.acceptanceStatus = b.acceptanceStatus;
        this.pickupPhotoUrl = b.pickupPhotoUrl;
        this.deliveryPhotoUrl = b.deliveryPhotoUrl;
        this.subStage = b.subStage;
        this.walletAmountUsed = b.walletAmountUsed;
        this.subtotal = b.subtotal;
        this.deliveryFee = b.deliveryFee;
        this.total = b.total;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderCode() { return orderCode; }
    public void setOrderCode(String orderCode) { this.orderCode = orderCode; }

    public User getCustomer() { return customer; }
    public void setCustomer(User customer) { this.customer = customer; }

    public User getDhobi() { return dhobi; }
    public void setDhobi(User dhobi) { this.dhobi = dhobi; }

    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public Double getPickupLatitude() { return pickupLatitude; }
    public void setPickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; }

    public Double getPickupLongitude() { return pickupLongitude; }
    public void setPickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; }

    public String getPickupSlot() { return pickupSlot; }
    public void setPickupSlot(String pickupSlot) { this.pickupSlot = pickupSlot; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getRazorpayOrderId() { return razorpayOrderId; }
    public void setRazorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; }

    public String getRazorpayPaymentId() { return razorpayPaymentId; }
    public void setRazorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; }

    public OrderStage getStage() { return stage; }
    public void setStage(OrderStage stage) { this.stage = stage; }

    public AcceptanceStatus getAcceptanceStatus() { return acceptanceStatus; }
    public void setAcceptanceStatus(AcceptanceStatus acceptanceStatus) { this.acceptanceStatus = acceptanceStatus; }

    public java.util.Set<Long> getDeclinedByDhobiIds() { return declinedByDhobiIds; }
    public void setDeclinedByDhobiIds(java.util.Set<Long> declinedByDhobiIds) { this.declinedByDhobiIds = declinedByDhobiIds; }

    public String getPickupPhotoUrl() { return pickupPhotoUrl; }
    public void setPickupPhotoUrl(String pickupPhotoUrl) { this.pickupPhotoUrl = pickupPhotoUrl; }

    public String getDeliveryPhotoUrl() { return deliveryPhotoUrl; }
    public void setDeliveryPhotoUrl(String deliveryPhotoUrl) { this.deliveryPhotoUrl = deliveryPhotoUrl; }

    /** Never returns null — legacy/unset orders read as NONE. */
    public WashSubStage getSubStage() { return subStage != null ? subStage : WashSubStage.NONE; }
    public void setSubStage(WashSubStage subStage) { this.subStage = subStage; }

    /** Never returns null — legacy/unset orders read as 0. */
    public int getWalletAmountUsed() { return walletAmountUsed != null ? walletAmountUsed : 0; }
    public void setWalletAmountUsed(Integer walletAmountUsed) { this.walletAmountUsed = walletAmountUsed; }

    public Integer getSubtotal() { return subtotal; }
    public void setSubtotal(Integer subtotal) { this.subtotal = subtotal; }

    public Integer getDeliveryFee() { return deliveryFee; }
    public void setDeliveryFee(Integer deliveryFee) { this.deliveryFee = deliveryFee; }

    public Integer getTotal() { return total; }
    public void setTotal(Integer total) { this.total = total; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public static class Builder {
        private Long id;
        private String orderCode;
        private User customer;
        private User dhobi;
        private List<OrderItem> items = new ArrayList<>();
        private String pickupAddress;
        private Double pickupLatitude;
        private Double pickupLongitude;
        private String pickupSlot;
        private String paymentMethod;
        private PaymentStatus paymentStatus = PaymentStatus.PENDING;
        private String razorpayOrderId;
        private String razorpayPaymentId;
        private OrderStage stage = OrderStage.PLACED;
        private AcceptanceStatus acceptanceStatus = AcceptanceStatus.PENDING;
        private String pickupPhotoUrl;
        private String deliveryPhotoUrl;
        private WashSubStage subStage;
        private Integer walletAmountUsed;
        private Integer subtotal;
        private Integer deliveryFee;
        private Integer total;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder orderCode(String orderCode) { this.orderCode = orderCode; return this; }
        public Builder customer(User customer) { this.customer = customer; return this; }
        public Builder dhobi(User dhobi) { this.dhobi = dhobi; return this; }
        public Builder items(List<OrderItem> items) { this.items = items; return this; }
        public Builder pickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; return this; }
        public Builder pickupLatitude(Double pickupLatitude) { this.pickupLatitude = pickupLatitude; return this; }
        public Builder pickupLongitude(Double pickupLongitude) { this.pickupLongitude = pickupLongitude; return this; }
        public Builder pickupSlot(String pickupSlot) { this.pickupSlot = pickupSlot; return this; }
        public Builder paymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; return this; }
        public Builder paymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; return this; }
        public Builder razorpayOrderId(String razorpayOrderId) { this.razorpayOrderId = razorpayOrderId; return this; }
        public Builder razorpayPaymentId(String razorpayPaymentId) { this.razorpayPaymentId = razorpayPaymentId; return this; }
        public Builder stage(OrderStage stage) { this.stage = stage; return this; }
        public Builder acceptanceStatus(AcceptanceStatus acceptanceStatus) { this.acceptanceStatus = acceptanceStatus; return this; }
        public Builder pickupPhotoUrl(String pickupPhotoUrl) { this.pickupPhotoUrl = pickupPhotoUrl; return this; }
        public Builder deliveryPhotoUrl(String deliveryPhotoUrl) { this.deliveryPhotoUrl = deliveryPhotoUrl; return this; }
        public Builder subStage(WashSubStage subStage) { this.subStage = subStage; return this; }
        public Builder walletAmountUsed(Integer walletAmountUsed) { this.walletAmountUsed = walletAmountUsed; return this; }
        public Builder subtotal(Integer subtotal) { this.subtotal = subtotal; return this; }
        public Builder deliveryFee(Integer deliveryFee) { this.deliveryFee = deliveryFee; return this; }
        public Builder total(Integer total) { this.total = total; return this; }

        public Order build() { return new Order(this); }
    }
}
