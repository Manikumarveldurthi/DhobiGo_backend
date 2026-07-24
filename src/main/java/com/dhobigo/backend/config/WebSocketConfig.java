package com.dhobigo.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Real-time channel, replacing the frontend's polling loops.
 *
 * Topics in use:
 *   /topic/orders/{orderId}          — order stage changes (tracking.html, dhobi.html)
 *   /topic/orders/{orderId}/messages — in-app chat between customer & dhobi
 *   /topic/dhobis/{dhobiId}/location — live dhobi location (future map view)
 *   /topic/admin/orders              — every order stage change, for admin.html's live table
 *
 * Every subscription is authorized per-order by StompAuthInterceptor — see
 * that class for why this matters (topic names alone aren't a security
 * boundary).
 *
 * The frontend connects via SockJS + STOMP.js (loaded from cdnjs) at
 * /ws — see js/realtime.js.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final StompAuthInterceptor stompAuthInterceptor;

    public WebSocketConfig(StompAuthInterceptor stompAuthInterceptor) {
        this.stompAuthInterceptor = stompAuthInterceptor;
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic"); // server -> client broadcast prefix
        registry.setApplicationDestinationPrefixes("/app"); // client -> server prefix (unused for now, reserved)
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*")
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthInterceptor);
    }
}
