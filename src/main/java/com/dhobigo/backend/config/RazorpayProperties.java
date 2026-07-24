package com.dhobigo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Real payment processing for UPI/Card, via Razorpay (the standard choice
 * for Indian apps — native UPI support, no international-merchant paperwork
 * needed to get started, and a generous test mode).
 *
 * To actually enable this, you need:
 *   1. A Razorpay account (razorpay.com) — sign up is free, test mode works
 *      immediately with no KYC needed
 *   2. Settings → API Keys → "Generate Test Key" — gives you a Key ID
 *      (starts with rzp_test_) and a Key Secret
 *   3. Set these environment variables:
 *        RAZORPAY_ENABLED=true
 *        RAZORPAY_KEY_ID=rzp_test_xxxxxxxx
 *        RAZORPAY_KEY_SECRET=your_key_secret
 *   4. For production: complete Razorpay's KYC/activation flow, then swap
 *      in your rzp_live_ key pair (same env vars, live values)
 *
 * Until RAZORPAY_ENABLED=true, "Pay & confirm pickup" on UPI/Card falls back
 * to demo mode (order is placed directly, marked PENDING) instead of
 * opening a real checkout — same safety pattern as WhatsApp/Email below.
 * Cash on delivery never touches this at all.
 */
@Component
@ConfigurationProperties(prefix = "app.razorpay")
public class RazorpayProperties {

    private boolean enabled = false;
    private String keyId;
    private String keySecret;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getKeyId() { return keyId; }
    public void setKeyId(String keyId) { this.keyId = keyId; }

    public String getKeySecret() { return keySecret; }
    public void setKeySecret(String keySecret) { this.keySecret = keySecret; }
}
