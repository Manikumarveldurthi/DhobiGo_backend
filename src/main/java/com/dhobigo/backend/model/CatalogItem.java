package com.dhobigo.backend.model;

import jakarta.persistence.*;

/**
 * Mirrors one entry in the frontend's CATALOG object (services-data.js):
 * { id, name, icon, price } scoped to a service type. Same field names on
 * purpose so the frontend needs minimal changes when it switches from the
 * hardcoded JS object to GET /api/catalog.
 */
@Entity
@Table(name = "catalog_item")
public class CatalogItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short slug like "shirt", "jeans" — matches frontend item.id */
    @Column(nullable = false)
    private String itemKey;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String icon;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ServiceType service;

    @Column(nullable = false)
    private Integer price;

    /** Eco-friendly / water-saving badge shown on services.html item cards. Nullable, defaults false. */
    private Boolean ecoFriendly;

    public CatalogItem() {
    }

    private CatalogItem(Builder b) {
        this.id = b.id;
        this.itemKey = b.itemKey;
        this.name = b.name;
        this.icon = b.icon;
        this.service = b.service;
        this.price = b.price;
        this.ecoFriendly = b.ecoFriendly;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getItemKey() { return itemKey; }
    public void setItemKey(String itemKey) { this.itemKey = itemKey; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public ServiceType getService() { return service; }
    public void setService(ServiceType service) { this.service = service; }

    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }

    /** Never returns null — legacy/unset rows default to false. */
    public boolean isEcoFriendly() { return ecoFriendly != null && ecoFriendly; }
    public void setEcoFriendly(Boolean ecoFriendly) { this.ecoFriendly = ecoFriendly; }

    public static class Builder {
        private Long id;
        private String itemKey;
        private String name;
        private String icon;
        private ServiceType service;
        private Integer price;
        private Boolean ecoFriendly;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder itemKey(String itemKey) { this.itemKey = itemKey; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder icon(String icon) { this.icon = icon; return this; }
        public Builder service(ServiceType service) { this.service = service; return this; }
        public Builder price(Integer price) { this.price = price; return this; }
        public Builder ecoFriendly(Boolean ecoFriendly) { this.ecoFriendly = ecoFriendly; return this; }

        public CatalogItem build() { return new CatalogItem(this); }
    }
}
