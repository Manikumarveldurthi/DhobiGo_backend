package com.dhobigo.backend.controller;

import com.dhobigo.backend.dto.PushDtos.PushSubscribeRequest;
import com.dhobigo.backend.dto.PushDtos.UnsubscribeRequest;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.service.PushNotificationService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Stores/removes browser Web Push subscriptions — any logged-in user. */
@RestController
@RequestMapping("/api/push")
public class PushController {

    private final PushNotificationService pushNotificationService;

    public PushController(PushNotificationService pushNotificationService) {
        this.pushNotificationService = pushNotificationService;
    }

    @PostMapping("/subscribe")
    public void subscribe(@AuthenticationPrincipal User user, @Valid @RequestBody PushSubscribeRequest req) {
        pushNotificationService.subscribe(user, req.endpoint(), req.p256dh(), req.auth());
    }

    @PostMapping("/unsubscribe")
    public void unsubscribe(@Valid @RequestBody UnsubscribeRequest req) {
        pushNotificationService.unsubscribe(req.endpoint());
    }
}
