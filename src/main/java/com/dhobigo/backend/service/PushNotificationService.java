package com.dhobigo.backend.service;

import com.dhobigo.backend.config.PushProperties;
import com.dhobigo.backend.model.PushSubscription;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.repository.PushSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * SAFE BY DEFAULT: subscribing/unsubscribing always works (real DB rows).
 * Actually pushing a notification is a stub that just logs unless
 * app.push.enabled is set — see PushProperties for what "going live" needs.
 * Called best-effort (wrapped in try/catch by callers) so a misconfigured
 * or absent push setup can never break order flow, same as WhatsApp.
 */
@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    private final PushProperties pushProperties;
    private final PushSubscriptionRepository pushSubscriptionRepository;

    public PushNotificationService(PushProperties pushProperties, PushSubscriptionRepository pushSubscriptionRepository) {
        this.pushProperties = pushProperties;
        this.pushSubscriptionRepository = pushSubscriptionRepository;
    }

    public void subscribe(User user, String endpoint, String p256dh, String auth) {
        if (pushSubscriptionRepository.findByEndpoint(endpoint).isPresent()) return; // already stored
        pushSubscriptionRepository.save(PushSubscription.builder()
                .user(user).endpoint(endpoint).p256dh(p256dh).auth(auth).build());
    }

    public void unsubscribe(String endpoint) {
        pushSubscriptionRepository.deleteByEndpoint(endpoint);
    }

    /** Sends (or, until configured, logs) a notification to every device this user has subscribed from. */
    public void notifyUser(Long userId, String title, String body) {
        try {
            var subs = pushSubscriptionRepository.findByUserId(userId);
            if (subs.isEmpty()) return;
            if (!pushProperties.isEnabled()) {
                log.info("[Push DISABLED — would send to {} device(s)] {}: {}", subs.size(), title, body);
                return;
            }
            // TODO once app.push.enabled=true: sign and POST each subscription's
            // endpoint per the Web Push protocol using pushProperties' VAPID
            // keys (a library like nl.martijndwars:webpush-java does this).
            log.info("[Push ENABLED but no send implementation wired in yet] {}: {}", title, body);
        } catch (Exception e) {
            log.warn("Push notification failed for user {}: {}", userId, e.getMessage());
        }
    }
}
