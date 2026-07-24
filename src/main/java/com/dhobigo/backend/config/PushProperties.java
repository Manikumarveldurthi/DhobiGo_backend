package com.dhobigo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Web Push (browser notifications even when the tab is closed).
 *
 * Storage of subscriptions (PushSubscription entity/controller) works today
 * with zero setup. Actually SENDING a push message needs VAPID keys and a
 * signed HTTP request per the Web Push protocol (RFC 8030) — deliberately
 * left as a stub in PushNotificationService (logs instead of sending, same
 * "disabled by default" pattern as WhatsApp/Razorpay/Email in this project)
 * rather than pinning an unverified extra Maven dependency here.
 *
 * To go live: add a web-push library (e.g. nl.martijndwars:webpush-java) to
 * pom.xml yourself, generate a VAPID keypair (that library has a CLI for
 * it), set these env vars, and fill in the real send call in
 * PushNotificationService.sendPush(...).
 */
@Component
@ConfigurationProperties(prefix = "app.push")
public class PushProperties {

    private boolean enabled = false;
    private String vapidPublicKey;
    private String vapidPrivateKey;
    private String vapidSubject = "mailto:admin@dhobigo.com";

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getVapidPublicKey() { return vapidPublicKey; }
    public void setVapidPublicKey(String vapidPublicKey) { this.vapidPublicKey = vapidPublicKey; }

    public String getVapidPrivateKey() { return vapidPrivateKey; }
    public void setVapidPrivateKey(String vapidPrivateKey) { this.vapidPrivateKey = vapidPrivateKey; }

    public String getVapidSubject() { return vapidSubject; }
    public void setVapidSubject(String vapidSubject) { this.vapidSubject = vapidSubject; }
}
