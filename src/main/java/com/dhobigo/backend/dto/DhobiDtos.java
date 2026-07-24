package com.dhobigo.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public class DhobiDtos {

    /** Customer-facing view — includes computed badges and, if the customer
        shared their location, distance in km. "phone" is only populated
        for the admin's view (see AdminController) — hidden from the
        public browse listing until a customer has actually booked them,
        same privacy pattern as OrderResponse revealing phone only once
        an order exists. */
    public record DhobiResponse(
            Long id,
            String fullName,
            Double rating,
            Integer completedOrders,
            Boolean available,
            Boolean approved,
            List<String> badges,
            Double distanceKm,
            String phone,
            Boolean enabled
    ) {}

    /** Admin's pending-approval queue view — includes contact info an admin needs to vet someone. */
    public record PendingDhobiResponse(
            Long id,
            String fullName,
            String email,
            String phone,
            String createdAt
    ) {}

    public record LocationUpdateRequest(
            @NotNull Double latitude,
            @NotNull Double longitude
    ) {}
}
