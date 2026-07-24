package com.dhobigo.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * To enable Google Sign-In:
 *   1. Go to console.cloud.google.com → create a project (free)
 *   2. APIs & Services → Credentials → Create Credentials → OAuth client ID
 *   3. Application type: Web application
 *   4. Add your frontend's origin (e.g. http://localhost:5500 or wherever
 *      you serve laundry-app/) under "Authorized JavaScript origins"
 *   5. Copy the Client ID (looks like xxxxx.apps.googleusercontent.com)
 *   6. Set it as GOOGLE_CLIENT_ID below AND in the frontend
 *      (js/api.js — GOOGLE_CLIENT_ID constant) — same value both places
 *
 * Unlike WhatsApp, this doesn't need business verification or a paid
 * account — it's free and works within minutes for a "Testing" app (Google
 * only requires review if you want it public-facing for external users at
 * scale, which isn't a concern for getting this working).
 */
@Component
@ConfigurationProperties(prefix = "app.google")
public class GoogleAuthProperties {

    private String clientId;

    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
}
