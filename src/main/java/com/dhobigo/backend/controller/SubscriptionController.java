package com.dhobigo.backend.controller;

import com.dhobigo.backend.dto.SubscriptionDtos.CreateSubscriptionRequest;
import com.dhobigo.backend.dto.SubscriptionDtos.SubscriptionResponse;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Customer-facing recurring pickup plans — subscriptions.html. */
@RestController
@RequestMapping("/api/subscriptions")
@PreAuthorize("hasRole('CUSTOMER')")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping
    public SubscriptionResponse create(@AuthenticationPrincipal User customer, @Valid @RequestBody CreateSubscriptionRequest req) {
        return subscriptionService.create(customer, req);
    }

    @GetMapping("/my")
    public List<SubscriptionResponse> myPlans(@AuthenticationPrincipal User customer) {
        return subscriptionService.myPlans(customer);
    }

    @PatchMapping("/{id}/pause")
    public SubscriptionResponse pause(@AuthenticationPrincipal User customer, @PathVariable Long id) {
        return subscriptionService.setActive(id, customer, false);
    }

    @PatchMapping("/{id}/resume")
    public SubscriptionResponse resume(@AuthenticationPrincipal User customer, @PathVariable Long id) {
        return subscriptionService.setActive(id, customer, true);
    }
}
