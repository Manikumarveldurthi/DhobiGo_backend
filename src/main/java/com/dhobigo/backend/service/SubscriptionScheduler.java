package com.dhobigo.backend.service;

import com.dhobigo.backend.model.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs once a day and sends a WhatsApp reminder for every recurring pickup
 * plan whose nextRunDate has arrived. Deliberately does NOT auto-place an
 * order (items vary each time) — see SubscriptionService's class comment.
 * Safe to leave running even with WhatsApp not configured: the underlying
 * WhatsAppNotificationService just logs in that case, same as everywhere
 * else in the app.
 */
@Component
public class SubscriptionScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionScheduler.class);

    private final SubscriptionService subscriptionService;
    private final WhatsAppNotificationService whatsAppNotificationService;

    public SubscriptionScheduler(SubscriptionService subscriptionService, WhatsAppNotificationService whatsAppNotificationService) {
        this.subscriptionService = subscriptionService;
        this.whatsAppNotificationService = whatsAppNotificationService;
    }

    @Scheduled(cron = "0 0 7 * * *") // every day at 7 AM server time
    public void remindDuePlans() {
        try {
            for (Subscription sub : subscriptionService.findDueToday()) {
                whatsAppNotificationService.sendOrderUpdate(
                        sub.getCustomer().getPhone(),
                        "DhobiGo: your recurring " + sub.getService().name().toLowerCase()
                                + " pickup is due today (slot: " + sub.getPickupSlot()
                                + "). Open the app's Subscriptions page to place today's order."
                );
            }
        } catch (Exception e) {
            // Never let a reminder-job failure affect the rest of the app.
            log.warn("Subscription reminder run failed: {}", e.getMessage());
        }
    }
}
