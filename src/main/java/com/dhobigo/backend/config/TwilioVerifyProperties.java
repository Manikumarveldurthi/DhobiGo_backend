package com.dhobigo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Phone-number login/signup via OTP, using Twilio Verify — a different
 * Twilio product from the WhatsApp messaging API, but the SAME Twilio
 * account/credentials you already set up for WhatsApp. You only need one
 * more thing:
 *
 *   1. In the Twilio Console, go to Verify → Services → Create new Service
 *      (free, instant — no business verification like WhatsApp senders)
 *   2. Name it anything (e.g. "DhobiGo OTP")
 *   3. Copy the Service SID (starts with "VA...")
 *   4. Set TWILIO_VERIFY_SERVICE_SID below
 *
 * Reuses WHATSAPP_ACCOUNT_SID / WHATSAPP_AUTH_TOKEN you already
 * configured — no new account credentials needed, just this one Service
 * SID.
 */
@Component
@ConfigurationProperties(prefix = "app.twilio-verify")
public class TwilioVerifyProperties {

    private boolean enabled = false;
    private String serviceSid;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getServiceSid() { return serviceSid; }
    public void setServiceSid(String serviceSid) { this.serviceSid = serviceSid; }
}
