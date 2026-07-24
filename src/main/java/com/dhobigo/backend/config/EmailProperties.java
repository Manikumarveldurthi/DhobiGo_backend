package com.dhobigo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Forgot-password emails. Disabled by default (logs the reset link instead
 * of emailing it) — same safe pattern as WhatsApp/Google/Twilio Verify.
 *
 * Easiest way to get this working: use a Gmail account with an "App
 * Password" (not your normal Gmail password):
 *   1. Enable 2-Step Verification on the Gmail account (required for App
 *      Passwords)
 *   2. Go to myaccount.google.com/apppasswords → generate one for "Mail"
 *   3. Set these environment variables:
 *        EMAIL_ENABLED=true
 *        SPRING_MAIL_HOST=smtp.gmail.com
 *        SPRING_MAIL_PORT=587
 *        SPRING_MAIL_USERNAME=youraddress@gmail.com
 *        SPRING_MAIL_PASSWORD=<the 16-character app password>
 *        EMAIL_FROM=youraddress@gmail.com
 *
 * No business verification needed, unlike WhatsApp — this works within
 * minutes on a normal Gmail account.
 */
@Component
@ConfigurationProperties(prefix = "app.email")
public class EmailProperties {

    private boolean enabled = false;
    private String from = "no-reply@dhobigo.local";
    /** Base URL the reset link points at — your frontend's location. */
    private String resetLinkBase = "http://localhost:5500/reset-password.html";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getResetLinkBase() { return resetLinkBase; }
    public void setResetLinkBase(String resetLinkBase) { this.resetLinkBase = resetLinkBase; }
}
