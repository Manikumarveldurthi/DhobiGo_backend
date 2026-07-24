package com.dhobigo.backend.controller;

import com.dhobigo.backend.dto.DhobiDtos.DhobiResponse;
import com.dhobigo.backend.dto.DhobiDtos.LocationUpdateRequest;
import com.dhobigo.backend.dto.OrderDtos.DeclineRequest;
import com.dhobigo.backend.dto.OrderDtos.OrderResponse;
import com.dhobigo.backend.dto.OrderDtos.StageUpdateRequest;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.service.DhobiService;
import com.dhobigo.backend.service.OrderService;
import com.dhobigo.backend.service.ReviewService;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Everything here requires role DHOBI (also enforced globally in
 * SecurityConfig for the /api/dhobi/** prefix — this @PreAuthorize is
 * belt-and-braces in case routing ever changes).
 */
@RestController
@RequestMapping("/api/dhobi")
@PreAuthorize("hasRole('DHOBI')")
public class DhobiActionController {

    private final OrderService orderService;
    private final DhobiService dhobiService;
    private final ReviewService reviewService;

    public DhobiActionController(OrderService orderService, DhobiService dhobiService, ReviewService reviewService) {
        this.orderService = orderService;
        this.dhobiService = dhobiService;
        this.reviewService = reviewService;
    }

    /** dhobi.html checks this right after login — are they approved yet, or still pending? */
    @GetMapping("/me")
    public DhobiResponse myProfile(@AuthenticationPrincipal User dhobi) {
        return dhobiService.getMyProfile(dhobi);
    }

    /** Pushed periodically from the browser's geolocation API while the dashboard is open. */
    @PatchMapping("/location")
    public void updateLocation(@AuthenticationPrincipal User dhobi, @Valid @RequestBody LocationUpdateRequest req) {
        dhobiService.updateLocation(dhobi, req.latitude(), req.longitude());
    }

    /** Dhobi manually toggles "online"/"offline" — only allowed once approved. */
    @PatchMapping("/availability")
    public void setAvailability(@AuthenticationPrincipal User dhobi, @RequestParam boolean available) {
        dhobiService.setAvailability(dhobi.getId(), available);
    }

    /** The dhobi.html dashboard's order queue — orders assigned to the logged-in dhobi. */
    @GetMapping("/orders")
    public List<OrderResponse> myAssignedOrders(@AuthenticationPrincipal User dhobi) {
        return orderService.getMyOrdersAsDhobi(dhobi);
    }

    /** Full order history (including delivered) — same data as /orders, kept
        as a separate name so the frontend's intent is clear: this is for
        the "my order history" view, not the active queue. */
    @GetMapping("/orders/history")
    public List<OrderResponse> myOrderHistory(@AuthenticationPrincipal User dhobi) {
        return orderService.getMyOrdersAsDhobi(dhobi);
    }

    /** Reviews customers have left for this dhobi. */
    @GetMapping("/reviews")
    public List<com.dhobigo.backend.dto.ReviewDtos.ReviewResponse> myReviews(@AuthenticationPrincipal User dhobi) {
        return reviewService.getForDhobi(dhobi.getId());
    }

    /** Advances an order one stage at a time — what the "Mark as: ..." button in dhobi.html should call. */
    @PatchMapping("/orders/{id}/stage")
    public OrderResponse advanceStage(
            @AuthenticationPrincipal User dhobi,
            @PathVariable Long id,
            @Valid @RequestBody StageUpdateRequest req
    ) {
        return orderService.updateStage(id, dhobi, req);
    }

    /** Dhobi taps "Accept" on a newly-assigned order — dhobi.html's order-queue card. */
    @PatchMapping("/orders/{id}/accept")
    public OrderResponse acceptOrder(@AuthenticationPrincipal User dhobi, @PathVariable Long id) {
        return orderService.acceptOrder(id, dhobi);
    }

    /**
     * Dhobi taps "Decline" instead — order is unassigned and the customer
     * gets notified to pick someone else from tracking.html. Body is
     * optional; a reason, if given, is included in that notification.
     */
    @PatchMapping("/orders/{id}/decline")
    public OrderResponse declineOrder(
            @AuthenticationPrincipal User dhobi,
            @PathVariable Long id,
            @RequestBody(required = false) DeclineRequest req
    ) {
        return orderService.declineOrder(id, dhobi, req);
    }

    /** Dhobi attaches a photo of the items collected — shown to the customer on tracking.html. */
    @PatchMapping("/orders/{id}/pickup-photo")
    public OrderResponse uploadPickupPhoto(
            @AuthenticationPrincipal User dhobi,
            @PathVariable Long id,
            @Valid @RequestBody com.dhobigo.backend.dto.OrderDtos.PhotoUploadRequest req
    ) {
        return orderService.updatePickupPhoto(id, dhobi, req.photoUrl());
    }

    /** Dhobi attaches a photo of the finished items before delivery. */
    @PatchMapping("/orders/{id}/delivery-photo")
    public OrderResponse uploadDeliveryPhoto(
            @AuthenticationPrincipal User dhobi,
            @PathVariable Long id,
            @Valid @RequestBody com.dhobigo.backend.dto.OrderDtos.PhotoUploadRequest req
    ) {
        return orderService.updateDeliveryPhoto(id, dhobi, req.photoUrl());
    }

    /** Garment-level micro-status (washing/drying/folding) while an order is in the WASHING stage. */
    @PatchMapping("/orders/{id}/substage")
    public OrderResponse updateSubStage(
            @AuthenticationPrincipal User dhobi,
            @PathVariable Long id,
            @Valid @RequestBody com.dhobigo.backend.dto.OrderDtos.SubStageUpdateRequest req
    ) {
        return orderService.updateSubStage(id, dhobi, req.subStage());
    }
}
