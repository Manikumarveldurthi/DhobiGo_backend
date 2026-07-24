package com.dhobigo.backend.model;

/** How often a recurring pickup plan (Subscription) repeats. */
public enum SubscriptionFrequency {
    WEEKLY(7),
    BIWEEKLY(14);

    private final int days;

    SubscriptionFrequency(int days) {
        this.days = days;
    }

    public int days() {
        return days;
    }
}
