package com.dhobigo.backend.controller;

import com.dhobigo.backend.dto.MessageDtos.MessageResponse;
import com.dhobigo.backend.dto.MessageDtos.SendMessageRequest;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST is used for loading chat history (on page load) and sending
 * messages; the WebSocket topic /topic/orders/{id}/messages is what
 * delivers new messages live to anyone already viewing the order.
 */
@RestController
@RequestMapping("/api/orders/{orderId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping
    public List<MessageResponse> getMessages(@AuthenticationPrincipal User user, @PathVariable Long orderId) {
        return messageService.getMessages(user, orderId);
    }

    @PostMapping
    public MessageResponse send(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId,
            @Valid @RequestBody SendMessageRequest req
    ) {
        return messageService.send(user, orderId, req.content());
    }
}
