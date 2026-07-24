package com.dhobigo.backend.config;

import com.dhobigo.backend.model.Order;
import com.dhobigo.backend.model.Role;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.repository.OrderRepository;
import com.dhobigo.backend.repository.UserRepository;
import com.dhobigo.backend.security.JwtService;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Without this, the /ws endpoint would broadcast every order's status and
 * chat messages to anyone who connects and guesses a sequential order ID
 * — STOMP topics have no built-in per-subscriber authorization. This
 * interceptor closes that gap:
 *
 *  - CONNECT: reads the JWT from the "Authorization" STOMP header (the
 *    frontend sends this the same way it does for REST calls) and attaches
 *    the resolved User to the session, or rejects the connection.
 *  - SUBSCRIBE: for /topic/orders/{id}... destinations, checks the
 *    connected user is that order's customer, its assigned dhobi, or an
 *    admin — otherwise throws, which closes the subscription attempt.
 *    /topic/admin/** requires ADMIN. Other topics (e.g. dhobi location)
 *    just require *some* authenticated user.
 */
@Component
public class StompAuthInterceptor implements ChannelInterceptor {

    private static final Pattern ORDER_TOPIC = Pattern.compile("^/topic/orders/(\\d+)(/.*)?$");
    private static final Pattern DHOBI_TOPIC = Pattern.compile("^/topic/dhobis/(\\d+)(/.*)?$");

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public StompAuthInterceptor(JwtService jwtService, UserRepository userRepository, OrderRepository orderRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = firstHeader(accessor, "Authorization");
            User user = resolveUser(authHeader);
            if (user == null) {
                throw new AccessDeniedException("Missing or invalid token on WebSocket connect");
            }
            accessor.getSessionAttributes().put("user", user);
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            User user = (User) accessor.getSessionAttributes().get("user");
            if (user == null) {
                throw new AccessDeniedException("Not authenticated");
            }
            String destination = accessor.getDestination();
            authorizeSubscription(user, destination);
        }

        return message;
    }

    private void authorizeSubscription(User user, String destination) {
        if (destination == null) return;

        if (destination.startsWith("/topic/admin/")) {
            if (user.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("Admin topic requires admin role");
            }
            return;
        }

        Matcher orderMatcher = ORDER_TOPIC.matcher(destination);
        if (orderMatcher.matches()) {
            Long orderId = Long.valueOf(orderMatcher.group(1));
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                throw new AccessDeniedException("Order not found");
            }
            boolean isCustomer = order.getCustomer().getId().equals(user.getId());
            boolean isDhobi = order.getDhobi() != null && order.getDhobi().getId().equals(user.getId());
            boolean isAdmin = user.getRole() == Role.ADMIN;
            if (!isCustomer && !isDhobi && !isAdmin) {
                throw new AccessDeniedException("Not authorized for this order's updates");
            }
            return;
        }

        Matcher dhobiMatcher = DHOBI_TOPIC.matcher(destination);
        if (dhobiMatcher.matches()) {
            Long dhobiId = Long.valueOf(dhobiMatcher.group(1));
            boolean isSelf = user.getId().equals(dhobiId);
            boolean isAdmin = user.getRole() == Role.ADMIN;
            // tracking.html's live map needs a customer to see their own
            // dhobi's position — without this, only the dhobi themself (or
            // an admin) could ever subscribe here.
            boolean isCustomerOfThisDhobi = orderRepository.existsByCustomerIdAndDhobiId(user.getId(), dhobiId);
            if (!isSelf && !isAdmin && !isCustomerOfThisDhobi) {
                throw new AccessDeniedException("Not authorized for this dhobi's channel");
            }
        }
    }

    private User resolveUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
        String token = authHeader.substring(7);
        try {
            if (!jwtService.isTokenValid(token)) return null;
            Long userId = jwtService.extractUserId(token);
            return userRepository.findById(userId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private String firstHeader(StompHeaderAccessor accessor, String name) {
        return Optional.ofNullable(accessor.getNativeHeader(name))
                .filter(list -> !list.isEmpty())
                .map(list -> list.get(0))
                .orElse(null);
    }
}
