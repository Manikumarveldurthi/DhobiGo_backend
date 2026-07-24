package com.dhobigo.backend.controller;

import com.dhobigo.backend.dto.OrderDtos.CreateOrderRequest;
import com.dhobigo.backend.dto.OrderDtos.OrderResponse;
import com.dhobigo.backend.dto.OrderDtos.ReassignRequest;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Customer creates a new order — the payment.html "Pay & confirm" action. */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse create(@AuthenticationPrincipal User customer, @Valid @RequestBody CreateOrderRequest req) {
        return orderService.createOrder(customer, req);
    }

    /** Any authenticated party involved in the order (customer, assigned dhobi, or admin) can view it. */
    @GetMapping("/{id}")
    public OrderResponse getById(@AuthenticationPrincipal User requester, @PathVariable Long id) {
        return orderService.getByIdForUser(id, requester);
    }

    /** Customer's own order history — powers a future "order history" page. */
    @GetMapping("/my")
    @PreAuthorize("hasRole('CUSTOMER')")
    public List<OrderResponse> myOrders(@AuthenticationPrincipal User customer) {
        return orderService.getMyOrdersAsCustomer(customer);
    }

    /**
     * Customer picks a replacement dhobi after their assigned one declined —
     * tracking.html's "choose another dhobi" panel calls this. Only allowed
     * while the order is actually in that DECLINED/unassigned state.
     */
    @PatchMapping("/{id}/reassign")
    @PreAuthorize("hasRole('CUSTOMER')")
    public OrderResponse reassign(
            @AuthenticationPrincipal User customer,
            @PathVariable Long id,
            @Valid @RequestBody ReassignRequest req
    ) {
        return orderService.reassignDhobi(id, customer, req);
    }
}
