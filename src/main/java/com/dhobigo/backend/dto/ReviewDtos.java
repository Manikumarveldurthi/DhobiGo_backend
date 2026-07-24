package com.dhobigo.backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class ReviewDtos {

    public record CreateReviewRequest(
            @NotNull @Min(1) @Max(5) Integer rating,
            @Size(max = 1000) String comment
    ) {}

    public record ReviewResponse(
            Long id,
            Long orderId,
            String orderCode,
            Long customerId,
            String customerName,
            Long dhobiId,
            String dhobiName,
            Integer rating,
            String comment,
            Instant createdAt
    ) {}
}
