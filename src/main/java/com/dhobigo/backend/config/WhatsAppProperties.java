package com.dhobigo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * WhatsApp notifications go through Twilio's WhatsApp API (the most common
 * route for developers — Twilio wraps Meta's WhatsApp Business Cloud API
 * so you don't need to apply for Meta developer access directly).
 *
 * To actually enable this, you need:
 *   1. A Twilio account (twilio.com) — free trial works for testing
 *   2. Their WhatsApp Sandbox (instant, for dev) or an approved
 *      WhatsApp Business sender (for production — takes Meta review time)
 *   3. Your Account SID + Auth Token from the Twilio console
 *
 * Until you set ACCOUNT_SID/AUTH_TOKEN, this stays disabled and every
 * "notification" is just logged instead of sent — nothing breaks, you
 * just won't get real WhatsApp messages until you plug in real credentials.
 */
@Component
@ConfigurationProperties(prefix = "app.whatsapp")
public class WhatsAppProperties {

    private boolean enabled = false;
    private String accountSid;
    private String authToken;
    /** Twilio sandbox default — replace once you have an approved sender. */
    private String fromNumber = "whatsapp:+14155238886";
    /** Prepended to phone numbers that don't already start with "+". */
    private String defaultCountryCode = "+91";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getAccountSid() { return accountSid; }
    public void setAccountSid(String accountSid) { this.accountSid = accountSid; }

    public String getAuthToken() { return authToken; }
    public void setAuthToken(String authToken) { this.authToken = authToken; }

    public String getFromNumber() { return fromNumber; }
    public void setFromNumber(String fromNumber) { this.fromNumber = fromNumber; }

    public String getDefaultCountryCode() { return defaultCountryCode; }
    public void setDefaultCountryCode(String defaultCountryCode) { this.defaultCountryCode = defaultCountryCode; }
}
