package com.dhobigo.backend.dto;

import com.dhobigo.backend.model.ServiceType;
import com.dhobigo.backend.model.SubscriptionFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class SubscriptionDtos {

    public record CreateSubscriptionRequest(
            @NotNull ServiceType service,
            @NotNull SubscriptionFrequency frequency,
            @NotBlank String pickupSlot,
            @NotBlank String pickupAddress
    ) {}

    public record SubscriptionResponse(
            Long id,
            ServiceType service,
            SubscriptionFrequency frequency,
            String pickupSlot,
            String pickupAddress,
            boolean active,
            LocalDate nextRunDate,
            // true when nextRunDate <= today — subscriptions.html shows an
            // "Order now" call-to-action prefilling services.html for this plan
            boolean dueToday
    ) {}
}
