package com.dhobigo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Forgot-password emails. Disabled by default (logs the reset link instead
 * of emailing it) — same safe pattern as WhatsApp/Google/Twilio Verify.
 *
 * Sent via the Resend HTTP API (https://resend.com) instead of raw SMTP —
 * SMTP ports (25/465/587) are blocked outbound on Render's free tier, but
 * Resend's API goes over normal HTTPS (443), so it works there too.
 *
 * To go live:
 *   1. Sign up free at resend.com (no credit card needed)
 *   2. Dashboard → API Keys → create one, copy it (starts with "re_")
 *   3. Set these environment variables:
 *        EMAIL_ENABLED=true
 *        RESEND_API_KEY=re_your_key_here
 *        EMAIL_FROM=onboarding@resend.dev
 *
 * NOTE: until you verify your own domain in Resend (Dashboard → Domains),
 * the sandbox address "onboarding@resend.dev" can only deliver to the
 * email address you signed up to Resend with. That's fine for testing the
 * flow yourself; to email real users, verify a domain and set EMAIL_FROM
 * to an address on it.
 */
@Component
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

    private boolean enabled = false;
    private String from = "no-reply@dhobigo.local";
    /** Base URL the reset link points at — your frontend's location. */
    private String resetLinkBase = "https://dhobigo.vercel.app/reset-password.html";
    /** Resend API key (starts with "re_"). See resend.com/api-keys. */
    private String resendApiKey = "re_cB63ixXv_CHngFuVMDVG4KF1m5w2YnMeg";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getResetLinkBase() { return resetLinkBase; }
    public void setResetLinkBase(String resetLinkBase) { this.resetLinkBase = resetLinkBase; }

    public String getResendApiKey() { return resendApiKey; }
    public void setResendApiKey(String resendApiKey) { this.resendApiKey = resendApiKey; }
}
