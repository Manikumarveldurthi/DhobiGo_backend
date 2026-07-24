package com.dhobigo.backend.controller;

import com.dhobigo.backend.dto.ReviewDtos.CreateReviewRequest;
import com.dhobigo.backend.dto.ReviewDtos.ReviewResponse;
import com.dhobigo.backend.exception.ApiException;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderId}/review")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /** Customer leaves a review — only allowed once the order is DELIVERED, one per order. */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ReviewResponse create(
            @AuthenticationPrincipal User customer,
            @PathVariable Long orderId,
            @Valid @RequestBody CreateReviewRequest req
    ) {
        return reviewService.create(customer, orderId, req);
    }

    /** Lets tracking.html check "has this order already been reviewed?" before showing the review form. */
    @GetMapping
    public ReviewResponse getForOrder(@PathVariable Long orderId) {
        return reviewService.getForOrder(orderId)
                .orElseThrow(() -> new ApiException("No review yet for this order", HttpStatus.NOT_FOUND));
    }
}
