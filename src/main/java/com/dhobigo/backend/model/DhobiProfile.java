package com.dhobigo.backend.model;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * 1:1 extension of a User with role=DHOBI. Kept separate from User so the
 * core auth table stays generic across all three roles.
 *
 * New dhobis start with approved=false, available=false — an admin must
 * approve them (see AdminController) before they show up to customers or
 * can be auto-assigned an order. This mirrors how Swiggy/Zomato onboard
 * delivery partners: signup != going live.
 */
@Entity
@Table(name = "dhobi_profile")
public class DhobiProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private Double rating = 5.0;

    @Column(nullable = false)
    private Integer completedOrders = 0;

    @Column(nullable = false)
    private Boolean available = false;

    @Column(nullable = false)
    private Boolean approved = false;

    private Instant approvedAt;

    // Live location — dhobi.html pushes this via browser geolocation while
    // the dhobi has the dashboard open, powering "dhobis near you".
    private Double latitude;
    private Double longitude;
    private Instant locationUpdatedAt;

    public DhobiProfile() {
    }

    private DhobiProfile(Builder b) {
        this.id = b.id;
        this.user = b.user;
        this.rating = b.rating;
        this.completedOrders = b.completedOrders;
        this.available = b.available;
        this.approved = b.approved;
        this.approvedAt = b.approvedAt;
        this.latitude = b.latitude;
        this.longitude = b.longitude;
        this.locationUpdatedAt = b.locationUpdatedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getCompletedOrders() { return completedOrders; }
    public void setCompletedOrders(Integer completedOrders) { this.completedOrders = completedOrders; }

    public Boolean getAvailable() { return available; }
    public void setAvailable(Boolean available) { this.available = available; }

    public Boolean getApproved() { return approved; }
    public void setApproved(Boolean approved) { this.approved = approved; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Instant getLocationUpdatedAt() { return locationUpdatedAt; }
    public void setLocationUpdatedAt(Instant locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; }

    public static class Builder {
        private Long id;
        private User user;
        private Double rating = 5.0;
        private Integer completedOrders = 0;
        private Boolean available = false;
        private Boolean approved = false;
        private Instant approvedAt;
        private Double latitude;
        private Double longitude;
        private Instant locationUpdatedAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder user(User user) { this.user = user; return this; }
        public Builder rating(Double rating) { this.rating = rating; return this; }
        public Builder completedOrders(Integer completedOrders) { this.completedOrders = completedOrders; return this; }
        public Builder available(Boolean available) { this.available = available; return this; }
        public Builder approved(Boolean approved) { this.approved = approved; return this; }
        public Builder approvedAt(Instant approvedAt) { this.approvedAt = approvedAt; return this; }
        public Builder latitude(Double latitude) { this.latitude = latitude; return this; }
        public Builder longitude(Double longitude) { this.longitude = longitude; return this; }
        public Builder locationUpdatedAt(Instant locationUpdatedAt) { this.locationUpdatedAt = locationUpdatedAt; return this; }

        public DhobiProfile build() { return new DhobiProfile(this); }
    }
}
