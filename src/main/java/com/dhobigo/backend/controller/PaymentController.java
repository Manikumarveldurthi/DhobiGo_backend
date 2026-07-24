package com.dhobigo.backend.controller;

import com.dhobigo.backend.dto.OrderDtos.CreateRazorpayOrderRequest;
import com.dhobigo.backend.dto.OrderDtos.RazorpayOrderResponse;
import com.dhobigo.backend.service.PaymentService;
import jakarta.validation.Valid;
import org.json.JSONObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final int DELIVERY_FEE = 25; // must match OrderService.DELIVERY_FEE / services-data.js

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /**
     * Called by payment.js right before opening Razorpay Checkout. The
     * amount is recomputed from the item list here (never trusted from the
     * client) so nobody can tamper with the price in the browser.
     *
     * If Razorpay isn't configured (app.razorpay.enabled=false), returns
     * enabled=false instead of erroring — payment.js falls back to demo
     * mode (order placed directly, no real charge) rather than breaking
     * the checkout flow for anyone who hasn't set up a Razorpay account yet.
     */
    @PostMapping("/create-order")
    @PreAuthorize("hasRole('CUSTOMER')")
    public RazorpayOrderResponse createOrder(@Valid @RequestBody CreateRazorpayOrderRequest req) {
        if (!paymentService.isEnabled()) {
            return new RazorpayOrderResponse(false, null, null, 0, null);
        }

        int subtotal = req.items().stream().mapToInt(i -> i.price() * i.qty()).sum();
        int total = subtotal + DELIVERY_FEE;

        JSONObject rzpOrder = paymentService.createOrder(total, "dhobigo-" + System.currentTimeMillis());

        return new RazorpayOrderResponse(
                true,
                paymentService.getPublicKeyId(),
                rzpOrder.getString("id"),
                rzpOrder.getInt("amount"),
                rzpOrder.getString("currency")
        );
    }
}
