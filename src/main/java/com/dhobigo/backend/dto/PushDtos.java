package com.dhobigo.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class PushDtos {

    /** Matches the shape of a browser PushSubscription.toJSON() object. */
    public record PushSubscribeRequest(
            @NotBlank String endpoint,
            @NotBlank String p256dh,
            @NotBlank String auth
    ) {}

    public record UnsubscribeRequest(
            @NotBlank String endpoint
    ) {}
}
