package com.dhobigo.backend.dto;

import com.dhobigo.backend.model.OrderStage;
import com.dhobigo.backend.model.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.Instant;
import java.util.List;

public class OrderDtos {

    public record OrderItemRequest(
            @NotBlank String itemKey,
            @NotBlank String name,
            ServiceType service,
            @Min(0) int price,
            @Min(1) int qty,
            // Optional — customer note/photo for this specific item (e.g. a stain to pretreat)
            String specialInstructions,
            String photoUrl
    ) {}

    /**
     * Field names deliberately match what payment.js currently builds
     * (dhobi, slot -> pickupSlot, address -> pickupAddress, payMethod) so
     * the frontend swap from localStorage to this endpoint is close to a
     * find-and-replace of the storage call, not a data reshape.
     */
    public record CreateOrderRequest(
            @NotEmpty @Valid List<OrderItemRequest> items,
            Long dhobiId, // optional — null means "auto-assign nearest available"
            @NotBlank String pickupSlot,
            @NotBlank String pickupAddress,
            // Optional — set when the customer used "Use current location" at
            // checkout instead of/alongside typing pickupAddress. When present
            // and dhobiId is null, auto-assign picks the nearest available
            // dhobi (Swiggy/Zomato-style), same as the browse listing does.
            Double pickupLatitude,
            Double pickupLongitude,
            @NotBlank String paymentMethod,
            // Only sent for "upi"/"card" once payment.js has completed a real
            // Razorpay Checkout flow (see PaymentController). Null for "cod"
            // and null in demo mode (Razorpay not configured on the backend) —
            // OrderService only demands these when it can actually verify them.
            String razorpayOrderId,
            String razorpayPaymentId,
            String razorpaySignature,
            // Optional — if true, apply as much of the customer's wallet balance
            // as possible toward this order's total before charging paymentMethod.
            boolean useWallet,
         // Optional — set when this order was placed via subscriptions.html's
            // "Order now" button, so we can advance that plan's next due date.
            Long subscriptionId
            
    ) {}

    /** Body for POST /api/payments/create-order — reuses the same item shape so the amount is computed server-side, never trusted from the client. */
    public record CreateRazorpayOrderRequest(
            @NotEmpty @Valid List<OrderItemRequest> items
    ) {}

    /** What payment.js needs to open Razorpay Checkout, or to know to fall back to demo mode. */
    public record RazorpayOrderResponse(
            boolean enabled,       // false = Razorpay isn't configured server-side; frontend should skip the real gateway
            String keyId,          // public key — safe to expose to the browser
            String razorpayOrderId,
            int amountPaise,
            String currency
    ) {}

    public record StageUpdateRequest(
            OrderStage stage
    ) {}

    /** Body for PATCH /api/dhobi/orders/{id}/decline — reason is optional, shown to the customer if present. */
    public record DeclineRequest(
            String reason
    ) {}

    /** Body for PATCH /api/orders/{id}/reassign — customer picking a replacement after a decline. */
    public record ReassignRequest(
            @jakarta.validation.constraints.NotNull Long dhobiId
    ) {}

    /** Body for PATCH /api/dhobi/orders/{id}/pickup-photo and .../delivery-photo. */
    public record PhotoUploadRequest(
            @NotBlank String photoUrl
    ) {}

    /** Body for PATCH /api/dhobi/orders/{id}/substage — garment-level status, only while stage = WASHING. */
    public record SubStageUpdateRequest(
            @jakarta.validation.constraints.NotNull com.dhobigo.backend.model.WashSubStage subStage
    ) {}

    public record OrderItemResponse(
            String itemKey,
            String name,
            ServiceType service,
            int price,
            int qty,
            String specialInstructions,
            String photoUrl
    ) {}

    public record OrderResponse(
            Long id,
            String orderCode,
            List<OrderItemResponse> items,
            String customerName,
            String customerPhone,
            String dhobiName,
            String dhobiPhone,
            Long dhobiId,           // lets tracking.html know which /topic/dhobis/{id}/location to subscribe to
            Double dhobiLatitude,   // last known position — null until the dhobi's dashboard has pushed one
            Double dhobiLongitude,
            String pickupSlot,
            String pickupAddress,
            Double pickupLatitude,
            Double pickupLongitude,
            String paymentMethod,
            com.dhobigo.backend.model.PaymentStatus paymentStatus,
            OrderStage stage,
            com.dhobigo.backend.model.AcceptanceStatus acceptanceStatus,
            String pickupPhotoUrl,
            String deliveryPhotoUrl,
            com.dhobigo.backend.model.WashSubStage subStage,
            int walletAmountUsed,
            int subtotal,
            int deliveryFee,
            int total,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
