package com.dhobigo.backend.model;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * One table for all three roles (CUSTOMER / DHOBI / ADMIN). Dhobi-specific
 * fields (rating, distance, availability) live in DhobiProfile, linked
 * 1:1, so this table stays clean for all users.
 */
@Entity
@Table(name = "app_user", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(unique = true)
    private String email;

    // Nullable — Google sign-in accounts don't collect this upfront, and
    // phone-OTP accounts have it but no email at first. Both flows should
    // prompt the user to complete their profile afterward (see
    // UserController's PATCH /api/users/me).
    private String phone;

    // Nullable — Google/OTP accounts never set a real password; they log
    // in only via those flows. isPasswordLogin() below reflects this.
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    // Extra profile fields — Swiggy/Zomato-style account completeness.
    private String address;

    // LONGTEXT — this can hold either a short image link or a full
    // base64 data: URL from the "upload a photo" option in profile.html
    // (client resizes to ~200px before sending, so it stays well under a
    // few hundred KB, but the default VARCHAR(255) would truncate it).
    @Column(columnDefinition = "LONGTEXT")
    private String avatarUrl;

    // ===== Added: referrals, wallet, corporate accounts (additive, all
    // nullable/defaulted so existing rows/flows are unaffected) =====

    /** This user's own shareable code — generated once at signup, never changes. */
    @Column(unique = true)
    private String referralCode;

    /** The referral code THIS user typed in at signup (if any) — kept for records. */
    private String referredByCode;

    /** "INDIVIDUAL" (default) or "CORPORATE" — see AuthDtos.RegisterRequest. Nullable
        so existing rows read as INDIVIDUAL via getAccountType(). */
    private String accountType;

    /** Only meaningful when accountType = CORPORATE (e.g. a hostel/hotel account). */
    private String companyName;

    /** Wallet balance in rupees. Nullable so existing rows default to 0 via getWalletBalance(). */
    private Integer walletBalance;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public User() {
    }

    private User(Builder b) {
        this.id = b.id;
        this.fullName = b.fullName;
        this.email = b.email;
        this.phone = b.phone;
        this.passwordHash = b.passwordHash;
        this.role = b.role;
        this.enabled = b.enabled;
        this.address = b.address;
        this.avatarUrl = b.avatarUrl;
        this.referralCode = b.referralCode;
        this.referredByCode = b.referredByCode;
        this.accountType = b.accountType;
        this.companyName = b.companyName;
        this.walletBalance = b.walletBalance;
        this.createdAt = b.createdAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    // ===== Getters / setters =====

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getReferralCode() { return referralCode; }
    public void setReferralCode(String referralCode) { this.referralCode = referralCode; }

    public String getReferredByCode() { return referredByCode; }
    public void setReferredByCode(String referredByCode) { this.referredByCode = referredByCode; }

    /** Never returns null — existing/legacy rows default to "INDIVIDUAL". */
    public String getAccountType() { return accountType != null ? accountType : "INDIVIDUAL"; }
    public void setAccountType(String accountType) { this.accountType = accountType; }

    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }

    /** Never returns null — existing/legacy rows default to 0. */
    public int getWalletBalance() { return walletBalance != null ? walletBalance : 0; }
    public void setWalletBalance(Integer walletBalance) { this.walletBalance = walletBalance; }

    public Instant getCreatedAt() { return createdAt; }

    public boolean hasPasswordLogin() { return passwordHash != null; }

    // ===== UserDetails contract =====

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email != null ? email : phone;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    // ===== Builder =====

    public static class Builder {
        private Long id;
        private String fullName;
        private String email;
        private String phone;
        private String passwordHash;
        private Role role;
        private boolean enabled = true;
        private String address;
        private String avatarUrl;
        private String referralCode;
        private String referredByCode;
        private String accountType;
        private String companyName;
        private Integer walletBalance;
        private Instant createdAt;

        public Builder id(Long id) { this.id = id; return this; }
        public Builder fullName(String fullName) { this.fullName = fullName; return this; }
        public Builder email(String email) { this.email = email; return this; }
        public Builder phone(String phone) { this.phone = phone; return this; }
        public Builder passwordHash(String passwordHash) { this.passwordHash = passwordHash; return this; }
        public Builder role(Role role) { this.role = role; return this; }
        public Builder enabled(boolean enabled) { this.enabled = enabled; return this; }
        public Builder address(String address) { this.address = address; return this; }
        public Builder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public Builder referralCode(String referralCode) { this.referralCode = referralCode; return this; }
        public Builder referredByCode(String referredByCode) { this.referredByCode = referredByCode; return this; }
        public Builder accountType(String accountType) { this.accountType = accountType; return this; }
        public Builder companyName(String companyName) { this.companyName = companyName; return this; }
        public Builder walletBalance(Integer walletBalance) { this.walletBalance = walletBalance; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        public User build() { return new User(this); }
    }
}
