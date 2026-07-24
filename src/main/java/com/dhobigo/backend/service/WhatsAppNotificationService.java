package com.dhobigo.backend.service;

import com.dhobigo.backend.config.WhatsAppProperties;
import com.dhobigo.backend.util.PhoneUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * Sends order-update messages over WhatsApp via Twilio's API.
 *
 * SAFE BY DEFAULT: if app.whatsapp.enabled is false (the default — see
 * WhatsAppProperties), this just logs what it would have sent instead of
 * making a real API call. Nothing breaks if you never configure this;
 * you only need to touch it when you're ready to send real messages.
 *
 * To go live:
 *   1. Sign up at twilio.com, get your Account SID + Auth Token
 *   2. Join their WhatsApp Sandbox for testing (instructions in the Twilio
 *      console — you WhatsApp a join code to their sandbox number once)
 *   3. Set these environment variables:
 *        WHATSAPP_ENABLED=true
 *        WHATSAPP_ACCOUNT_SID=ACxxxxxxxx...
 *        WHATSAPP_AUTH_TOKEN=your_auth_token
 *   4. For production (not just your own testing), apply for an approved
 *      WhatsApp Business sender through Twilio — this involves Meta's
 *      review process and takes some days; the sandbox only lets you
 *      message numbers that have explicitly joined it.
 */
@Service
public class WhatsAppNotificationService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppNotificationService.class);
    private static final String TWILIO_API = "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json";

    private final WhatsAppProperties props;
    private final RestTemplate restTemplate = new RestTemplate();

    public WhatsAppNotificationService(WhatsAppProperties props) {
        this.props = props;
    }

    public void sendOrderUpdate(String rawPhone, String message) {
        String to = PhoneUtil.normalize(rawPhone, props.getDefaultCountryCode());

        if (!props.isEnabled()) {
            log.info("[WhatsApp DISABLED — would send] to={} message=\"{}\"", to, message);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.setBasicAuth(props.getAccountSid(), props.getAuthToken());

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("From", props.getFromNumber());
            body.add("To", "whatsapp:" + to);
            body.add("Body", message);

            String url = String.format(TWILIO_API, props.getAccountSid());
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
            log.info("WhatsApp message sent to {}", to);
        } catch (Exception e) {
            // Never let a failed notification break the actual order flow —
            // log and move on. The order/stage update itself already
            // succeeded before this is called.
            log.warn("Failed to send WhatsApp message to {}: {}", to, e.getMessage());
        }
    }
}
