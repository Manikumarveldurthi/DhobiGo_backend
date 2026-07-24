package com.dhobigo.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public class MessageDtos {

    public record SendMessageRequest(
            @NotBlank @Size(max = 1000) String content
    ) {}

    public record MessageResponse(
            Long id,
            Long orderId,
            Long senderId,
            String senderName,
            String senderRole,
            String content,
            Instant createdAt
    ) {}
}
