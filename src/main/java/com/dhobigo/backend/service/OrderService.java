package com.dhobigo.backend.service;

import com.dhobigo.backend.dto.OrderDtos.*;
import com.dhobigo.backend.exception.ApiException;
import com.dhobigo.backend.model.*;
import com.dhobigo.backend.repository.DhobiProfileRepository;
import com.dhobigo.backend.repository.OrderRepository;
import com.dhobigo.backend.repository.SubscriptionRepository;
import com.dhobigo.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;

@Service
public class OrderService {

    private static final int DELIVERY_FEE = 25; // must match DELIVERY_FEE in services-data.js
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final DhobiProfileRepository dhobiProfileRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final WhatsAppNotificationService whatsAppNotificationService;
    private final PaymentService paymentService;
    private final WalletService walletService;

    private final SubscriptionRepository subscriptionRepository;

    public OrderService(OrderRepository orderRepository,
                         UserRepository userRepository,
                         DhobiProfileRepository dhobiProfileRepository,
                         SimpMessagingTemplate messagingTemplate,
                         WhatsAppNotificationService whatsAppNotificationService,
                         PaymentService paymentService,
                         WalletService walletService,
                         SubscriptionRepository subscriptionRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.dhobiProfileRepository = dhobiProfileRepository;
        this.messagingTemplate = messagingTemplate;
        this.whatsAppNotificationService = whatsAppNotificationService;
        this.paymentService = paymentService;
        this.walletService = walletService;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public OrderResponse createOrder(User customer, CreateOrderRequest req) {
        User dhobi = resolveDhobi(req.dhobiId(), req.pickupLatitude(), req.pickupLongitude());

        int subtotal = req.items().stream().mapToInt(i -> i.price() * i.qty()).sum();
        int total = subtotal + DELIVERY_FEE;

        PaymentStatus paymentStatus = resolvePaymentStatus(req);
        String orderCode = generateOrderCode();

        // Wallet is applied against the total up-front so it's visible in
        // the very first WhatsApp/order-summary message. Never blocks
        // checkout — applies whatever it can (possibly 0) and continues.
        int walletUsed = req.useWallet() ? walletService.applyToOrder(customer, total, orderCode) : 0;

        Order order = Order.builder()
                .orderCode(orderCode)
                .customer(customer)
                .dhobi(dhobi)
                .pickupAddress(req.pickupAddress())
                .pickupLatitude(req.pickupLatitude())
                .pickupLongitude(req.pickupLongitude())
                .pickupSlot(req.pickupSlot())
                .paymentMethod(req.paymentMethod())
                .paymentStatus(paymentStatus)
                .razorpayOrderId(req.razorpayOrderId())
                .razorpayPaymentId(req.razorpayPaymentId())
                .stage(OrderStage.PLACED)
                .subtotal(subtotal)
                .deliveryFee(DELIVERY_FEE)
                .total(total)
                .walletAmountUsed(walletUsed)
                .build();

        req.items().forEach(i -> order.addItem(
                OrderItem.builder()
                        .itemKey(i.itemKey())
                        .name(i.name())
                        .service(i.service())
                        .price(i.price())
                        .qty(i.qty())
                        .specialInstructions(i.specialInstructions())
                        .photoUrl(i.photoUrl())
                        .build()
        ));

        Order savedOrder = orderRepository.save(order);

     // If this order came from a recurring plan's "Order now" button, push
     // that plan's next due date forward so it stops reminding until the
     // next cycle — only if it actually belongs to this customer.
     if (req.subscriptionId() != null) {
         subscriptionRepository.findById(req.subscriptionId())
                 .filter(sub -> sub.getCustomer().getId().equals(customer.getId()))
                 .ifPresent(sub -> {
                     sub.setNextRunDate(sub.getNextRunDate().plusDays(sub.getFrequency().days()));
                     subscriptionRepository.save(sub);
                 });
     }

     OrderResponse response = toResponse(savedOrder);
        messagingTemplate.convertAndSend("/topic/admin/orders", response);
        messagingTemplate.convertAndSend("/topic/dhobis/" + dhobi.getId() + "/orders", response);

        whatsAppNotificationService.sendOrderUpdate(
                dhobi.getPhone(),
                String.format("DhobiGo: new pickup assigned — order #%s, slot %s.", savedOrder.getOrderCode(), req.pickupSlot())
        );

        whatsAppNotificationService.sendOrderUpdate(
                customer.getPhone(),
                buildOrderSummaryMessage(response)
        );

        return response;
    }

    /** Human-readable order summary for the "order placed" WhatsApp message. */
    private String buildOrderSummaryMessage(OrderResponse order) {
        StringBuilder sb = new StringBuilder();
        sb.append("DhobiGo order #").append(order.orderCode()).append(" confirmed!\n\n");
        for (OrderItemResponse item : order.items()) {
            sb.append("- ").append(item.name()).append(" x").append(item.qty())
                    .append(" (₹").append(item.price() * item.qty()).append(")\n");
        }
        sb.append("\nSubtotal: ₹").append(order.subtotal());
        sb.append("\nDelivery: ₹").append(order.deliveryFee());
        sb.append("\nTotal: ₹").append(order.total());
        sb.append("\nPickup: ").append(order.pickupSlot());
        sb.append("\nAddress: ").append(order.pickupAddress());
        if (order.dhobiName() != null) {
            sb.append("\nDhobi: ").append(order.dhobiName());
        }
        return sb.toString();
    }

    public OrderResponse getByIdForUser(Long orderId, User requester) {
        Order order = findOrderOrThrow(orderId);
        assertCanView(order, requester);
        return toResponse(order);
    }

    public List<OrderResponse> getMyOrdersAsCustomer(User customer) {
        return orderRepository.findByCustomerIdOrderByCreatedAtDesc(customer.getId())
                .stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> getMyOrdersAsDhobi(User dhobi) {
        return orderRepository.findByDhobiIdOrderByCreatedAtDesc(dhobi.getId())
                .stream().map(this::toResponse).toList();
    }

    public List<OrderResponse> getAllOrdersForAdmin() {
        return orderRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public OrderResponse updateStage(Long orderId, User dhobi, StageUpdateRequest req) {
        Order order = findOrderOrThrow(orderId);
        assertAssignedDhobi(order, dhobi);

        if (order.getAcceptanceStatus() == AcceptanceStatus.PENDING) {
            throw new ApiException("Accept this order before updating its status", HttpStatus.BAD_REQUEST);
        }
        if (order.getAcceptanceStatus() == AcceptanceStatus.DECLINED) {
            throw new ApiException("You've declined this order", HttpStatus.BAD_REQUEST);
        }

        OrderStage requested = req.stage();
        if (requested.ordinal() < order.getStage().ordinal()) {
            throw new ApiException("Cannot move an order backward in its timeline", HttpStatus.BAD_REQUEST);
        }
        if (requested.ordinal() > order.getStage().ordinal() + 1) {
            throw new ApiException("Stages must be advanced one at a time", HttpStatus.BAD_REQUEST);
        }

        order.setStage(requested);
        // Garment-level sub-status only makes sense during WASHING —
        // seed a sensible default entering it, clear it leaving it.
        order.setSubStage(requested == OrderStage.WASHING ? WashSubStage.WASHING : WashSubStage.NONE);

        if (requested == OrderStage.DELIVERED) {
            dhobiProfileRepository.findByUserId(dhobi.getId()).ifPresent(profile -> {
                profile.setCompletedOrders(profile.getCompletedOrders() + 1);
                dhobiProfileRepository.save(profile);
            });
        }

        OrderResponse response = toResponse(order);
        // Real-time push — anyone on tracking.html/dhobi.html for this
        // order sees the new stage immediately, no polling needed.
        messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), response);
        messagingTemplate.convertAndSend("/topic/admin/orders", response);

        whatsAppNotificationService.sendOrderUpdate(
                order.getCustomer().getPhone(),
                String.format("DhobiGo order #%s update: %s", order.getOrderCode(), requested.label())
        );

        return response;
    }

    /** Dhobi taps "Accept" on a newly-assigned order in their queue. */
    @Transactional
    public OrderResponse acceptOrder(Long orderId, User dhobi) {
        Order order = findOrderOrThrow(orderId);
        assertAssignedDhobi(order, dhobi);

        if (order.getAcceptanceStatus() != AcceptanceStatus.PENDING) {
            throw new ApiException("This order isn't waiting for a response", HttpStatus.BAD_REQUEST);
        }

        order.setAcceptanceStatus(AcceptanceStatus.ACCEPTED);

        OrderResponse response = toResponse(order);
        messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), response);
        messagingTemplate.convertAndSend("/topic/admin/orders", response);

        whatsAppNotificationService.sendOrderUpdate(
                order.getCustomer().getPhone(),
                String.format("DhobiGo: %s accepted your order #%s and will be on the way soon.",
                        dhobi.getFullName(), order.getOrderCode())
        );

        return response;
    }

    /**
     * Dhobi taps "Decline" instead. Per how this app works today, this does
     * NOT auto-reassign — the order is unassigned and the customer is
     * notified to pick a different dhobi themselves from tracking.html
     * (mirrors a real delivery app's "restaurant/driver declined, choose
     * another" flow). The declining dhobi is remembered so the customer
     * can't be routed back to them by mistake.
     */
    @Transactional
    public OrderResponse declineOrder(Long orderId, User dhobi, DeclineRequest req) {
        Order order = findOrderOrThrow(orderId);
        assertAssignedDhobi(order, dhobi);

        if (order.getAcceptanceStatus() != AcceptanceStatus.PENDING) {
            throw new ApiException("This order isn't waiting for a response", HttpStatus.BAD_REQUEST);
        }

        order.getDeclinedByDhobiIds().add(dhobi.getId());
        order.setDhobi(null);
        order.setAcceptanceStatus(AcceptanceStatus.DECLINED);

        OrderResponse response = toResponse(order);
        // Real-time — tracking.html reacts immediately by showing the
        // "choose another dhobi" panel instead of waiting on the next poll.
        messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), response);
        messagingTemplate.convertAndSend("/topic/admin/orders", response);

        String reasonSuffix = (req != null && req.reason() != null && !req.reason().isBlank())
                ? (" (" + req.reason().trim() + ")")
                : "";
        whatsAppNotificationService.sendOrderUpdate(
                order.getCustomer().getPhone(),
                String.format("DhobiGo: your dhobi wasn't able to take order #%s%s. Please open the app and choose another dhobi to continue.",
                        order.getOrderCode(), reasonSuffix)
        );

        return response;
    }

    /** Customer picks a replacement dhobi on tracking.html after a decline. */
    @Transactional
    public OrderResponse reassignDhobi(Long orderId, User customer, ReassignRequest req) {
        Order order = findOrderOrThrow(orderId);
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new ApiException("You don't have access to this order", HttpStatus.FORBIDDEN);
        }
        if (order.getAcceptanceStatus() != AcceptanceStatus.DECLINED) {
            throw new ApiException("This order isn't waiting for a new dhobi", HttpStatus.BAD_REQUEST);
        }
        if (order.getDeclinedByDhobiIds().contains(req.dhobiId())) {
            throw new ApiException("That dhobi already declined this order — please pick someone else", HttpStatus.BAD_REQUEST);
        }

        User newDhobi = userRepository.findById(req.dhobiId())
                .orElseThrow(() -> new ApiException("Selected dhobi not found", HttpStatus.NOT_FOUND));
        if (newDhobi.getRole() != Role.DHOBI) {
            throw new ApiException("Selected user is not a dhobi", HttpStatus.BAD_REQUEST);
        }
        DhobiProfile profile = dhobiProfileRepository.findByUserId(newDhobi.getId())
                .orElseThrow(() -> new ApiException("Dhobi profile not found", HttpStatus.NOT_FOUND));
        if (!Boolean.TRUE.equals(profile.getApproved())) {
            throw new ApiException("Selected dhobi is not yet approved", HttpStatus.BAD_REQUEST);
        }

        order.setDhobi(newDhobi);
        order.setAcceptanceStatus(AcceptanceStatus.PENDING);

        OrderResponse response = toResponse(order);
        messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), response);
        messagingTemplate.convertAndSend("/topic/admin/orders", response);
        messagingTemplate.convertAndSend("/topic/dhobis/" + newDhobi.getId() + "/orders", response);

        whatsAppNotificationService.sendOrderUpdate(
                newDhobi.getPhone(),
                String.format("DhobiGo: new pickup assigned — order #%s, slot %s.", order.getOrderCode(), order.getPickupSlot())
        );

        return response;
    }

    /** Dhobi uploads a photo of the items at pickup — trust/dispute-resolution feature. */
    @Transactional
    public OrderResponse updatePickupPhoto(Long orderId, User dhobi, String photoUrl) {
        Order order = findOrderOrThrow(orderId);
        assertAssignedDhobi(order, dhobi);
        if (order.getStage().ordinal() < OrderStage.COLLECTED.ordinal()) {
            throw new ApiException("Mark the order as collected before attaching a pickup photo", HttpStatus.BAD_REQUEST);
        }
        order.setPickupPhotoUrl(photoUrl);
        OrderResponse response = toResponse(order);
        messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), response);
        return response;
    }

    /** Dhobi uploads a photo of the finished items before delivery. */
    @Transactional
    public OrderResponse updateDeliveryPhoto(Long orderId, User dhobi, String photoUrl) {
        Order order = findOrderOrThrow(orderId);
        assertAssignedDhobi(order, dhobi);
        if (order.getStage().ordinal() < OrderStage.IRONED_PACKED.ordinal()) {
            throw new ApiException("Mark the order as ironed & packed before attaching a delivery photo", HttpStatus.BAD_REQUEST);
        }
        order.setDeliveryPhotoUrl(photoUrl);
        OrderResponse response = toResponse(order);
        messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), response);
        return response;
    }

    /** Garment-level micro-status (washing/drying/folding) — only while the order is in the WASHING stage. */
    @Transactional
    public OrderResponse updateSubStage(Long orderId, User dhobi, WashSubStage subStage) {
        Order order = findOrderOrThrow(orderId);
        assertAssignedDhobi(order, dhobi);
        if (order.getStage() != OrderStage.WASHING) {
            throw new ApiException("Sub-status only applies while the order is being washed", HttpStatus.BAD_REQUEST);
        }
        order.setSubStage(subStage);
        OrderResponse response = toResponse(order);
        messagingTemplate.convertAndSend("/topic/orders/" + order.getId(), response);
        return response;
    }

    // ===== helpers =====

    private void assertAssignedDhobi(Order order, User dhobi) {
        if (order.getDhobi() == null || !order.getDhobi().getId().equals(dhobi.getId())) {
            throw new ApiException("You are not assigned to this order", HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Cash on delivery never touches Razorpay — straight to COD.
     * For UPI/Card: if the gateway is configured and the client sent back
     * Razorpay's order/payment/signature triplet, verify it server-side
     * (throws if invalid — order creation aborts, nothing is charged twice).
     * If the gateway isn't configured at all, fall back to demo mode
     * (PENDING, no verification) so the checkout flow still works for
     * anyone who hasn't set up Razorpay credentials yet.
     */
    private PaymentStatus resolvePaymentStatus(CreateOrderRequest req) {
        if ("cod".equalsIgnoreCase(req.paymentMethod())) {
            return PaymentStatus.COD;
        }
        if (!paymentService.isEnabled()) {
            return PaymentStatus.PENDING; // demo mode
        }
        paymentService.verifySignature(req.razorpayOrderId(), req.razorpayPaymentId(), req.razorpaySignature());
        return PaymentStatus.PAID;
    }

    private User resolveDhobi(Long requestedDhobiId, Double customerLat, Double customerLng) {
        if (requestedDhobiId != null) {
            User dhobi = userRepository.findById(requestedDhobiId)
                    .orElseThrow(() -> new ApiException("Selected dhobi not found", HttpStatus.NOT_FOUND));
            if (dhobi.getRole() != Role.DHOBI) {
                throw new ApiException("Selected user is not a dhobi", HttpStatus.BAD_REQUEST);
            }
            DhobiProfile profile = dhobiProfileRepository.findByUserId(dhobi.getId())
                    .orElseThrow(() -> new ApiException("Dhobi profile not found", HttpStatus.NOT_FOUND));
            if (!Boolean.TRUE.equals(profile.getApproved())) {
                throw new ApiException("Selected dhobi is not yet approved", HttpStatus.BAD_REQUEST);
            }
            return dhobi;
        }

        List<DhobiProfile> candidates = dhobiProfileRepository.findByApprovedTrueAndAvailableTrue();

        // Auto-assign, Swiggy/Zomato-style: if the customer's pickup
        // coordinates are known (typed address has none, "use current
        // location" does), pick whichever available dhobi is physically
        // closest right now. Otherwise fall back to the first available one.
        if (customerLat != null && customerLng != null) {
            return candidates.stream()
                    .filter(p -> p.getLatitude() != null && p.getLongitude() != null)
                    .min(java.util.Comparator.comparingDouble(p ->
                            com.dhobigo.backend.util.GeoUtil.distanceKm(customerLat, customerLng, p.getLatitude(), p.getLongitude())))
                    .or(() -> candidates.stream().findFirst())
                    .map(DhobiProfile::getUser)
                    .orElseThrow(() -> new ApiException("No dhobis available right now", HttpStatus.SERVICE_UNAVAILABLE));
        }

        return candidates.stream()
                .findFirst()
                .map(DhobiProfile::getUser)
                .orElseThrow(() -> new ApiException("No dhobis available right now", HttpStatus.SERVICE_UNAVAILABLE));
    }

    private void assertCanView(Order order, User requester) {
        boolean isCustomer = order.getCustomer().getId().equals(requester.getId());
        boolean isAssignedDhobi = order.getDhobi() != null && order.getDhobi().getId().equals(requester.getId());
        boolean isAdmin = requester.getRole() == Role.ADMIN;
        if (!isCustomer && !isAssignedDhobi && !isAdmin) {
            throw new ApiException("You don't have access to this order", HttpStatus.FORBIDDEN);
        }
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));
    }

    private String generateOrderCode() {
        int suffix = 10000 + RANDOM.nextInt(89999);
        return "DG-" + suffix;
    }

    private OrderResponse toResponse(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(i -> new OrderItemResponse(i.getItemKey(), i.getName(), i.getService(), i.getPrice(), i.getQty(),
                        i.getSpecialInstructions(), i.getPhotoUrl()))
                .toList();

        Long dhobiId = order.getDhobi() != null ? order.getDhobi().getId() : null;
        Double dhobiLat = null;
        Double dhobiLng = null;
        if (dhobiId != null) {
            // Best-effort — a dhobi profile should always exist for a
            // DHOBI-role user, but don't let a missing one break order
            // loading; the map just stays in its "waiting for location" state.
            DhobiProfile profile = dhobiProfileRepository.findByUserId(dhobiId).orElse(null);
            if (profile != null) {
                dhobiLat = profile.getLatitude();
                dhobiLng = profile.getLongitude();
            }
        }

        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                items,
                order.getCustomer().getFullName(),
                order.getCustomer().getPhone(),
                order.getDhobi() != null ? order.getDhobi().getFullName() : null,
                order.getDhobi() != null ? order.getDhobi().getPhone() : null,
                dhobiId,
                dhobiLat,
                dhobiLng,
                order.getPickupSlot(),
                order.getPickupAddress(),
                order.getPickupLatitude(),
                order.getPickupLongitude(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getStage(),
                order.getAcceptanceStatus(),
                order.getPickupPhotoUrl(),
                order.getDeliveryPhotoUrl(),
                order.getSubStage(),
                order.getWalletAmountUsed(),
                order.getSubtotal(),
                order.getDeliveryFee(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
