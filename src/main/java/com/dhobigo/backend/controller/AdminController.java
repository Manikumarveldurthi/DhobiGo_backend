package com.dhobigo.backend.controller;

import com.dhobigo.backend.dto.CatalogDtos.CatalogItemRequest;
import com.dhobigo.backend.dto.CatalogDtos.CatalogItemResponse;
import com.dhobigo.backend.dto.DhobiDtos.DhobiResponse;
import com.dhobigo.backend.dto.DhobiDtos.PendingDhobiResponse;
import com.dhobigo.backend.dto.OrderDtos.OrderResponse;
import com.dhobigo.backend.dto.ReviewDtos.ReviewResponse;
import com.dhobigo.backend.model.DhobiProfile;
import com.dhobigo.backend.model.Role;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.repository.DhobiProfileRepository;
import com.dhobigo.backend.repository.UserRepository;
import com.dhobigo.backend.service.CatalogService;
import com.dhobigo.backend.service.DhobiService;
import com.dhobigo.backend.service.OrderService;
import com.dhobigo.backend.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Everything here requires role ADMIN (also enforced globally in
 * SecurityConfig for the /api/admin/** prefix).
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OrderService orderService;
    private final UserRepository userRepository;
    private final DhobiProfileRepository dhobiProfileRepository;
    private final DhobiService dhobiService;
    private final CatalogService catalogService;
    private final ReviewService reviewService;

    public AdminController(OrderService orderService,
                            UserRepository userRepository,
                            DhobiProfileRepository dhobiProfileRepository,
                            DhobiService dhobiService,
                            CatalogService catalogService,
                            ReviewService reviewService) {
        this.orderService = orderService;
        this.userRepository = userRepository;
        this.dhobiProfileRepository = dhobiProfileRepository;
        this.dhobiService = dhobiService;
        this.catalogService = catalogService;
        this.reviewService = reviewService;
    }

    // ===== Orders =====

    /** Every order in the system — for a future admin console table. */
    @GetMapping("/orders")
    public List<OrderResponse> allOrders() {
        return orderService.getAllOrdersForAdmin();
    }

    // ===== Users =====

    @GetMapping("/users/customers")
    public List<UserSummary> allCustomers() {
        return userRepository.findByRole(Role.CUSTOMER).stream().map(this::toSummary).toList();
    }

    @GetMapping("/users/dhobis")
    public List<DhobiResponse> allDhobis() {
        return dhobiProfileRepository.findAll().stream()
                .map(p -> new DhobiResponse(
                        p.getUser().getId(), p.getUser().getFullName(), p.getRating(),
                        p.getCompletedOrders(), p.getAvailable(), p.getApproved(), List.of(), null,
                        p.getUser().getPhone(), p.getUser().isEnabled()))
                .toList();
    }

    // ===== Dhobi approval workflow =====

    /** New dhobi signups awaiting review — this is the "approval of new dhobis" screen. */
    @GetMapping("/dhobis/pending")
    public List<PendingDhobiResponse> pendingDhobis() {
        return dhobiService.getPendingApprovals();
    }

    @PatchMapping("/dhobis/{userId}/approve")
    public void approveDhobi(@PathVariable Long userId) {
        dhobiService.approve(userId);
    }

    @PatchMapping("/dhobis/{userId}/reject")
    public void rejectDhobi(@PathVariable Long userId) {
        dhobiService.reject(userId);
    }

    /** Force a live dhobi offline (e.g. complaints, no-shows) without a full reject. */
    @PatchMapping("/dhobis/{userId}/availability")
    public void setDhobiAvailability(@PathVariable Long userId, @RequestParam boolean available) {
        dhobiService.setAvailability(userId, available);
    }

    /**
     * Removes a dhobi from the live site entirely — blocks login, hides
     * them from customer browsing/auto-assignment. Doesn't hard-delete the
     * row (that would break historical orders/reviews referencing them),
     * just deactivates. Meant to be used based on poor reviews/ratings or
     * policy violations.
     */
    @PatchMapping("/dhobis/{userId}/ban")
    public void banDhobi(@PathVariable Long userId) {
        dhobiService.ban(userId);
    }

    /** Reverses a ban — dhobi can log in again, but stays unapproved until
        re-approved through the normal approval flow. */
    @PatchMapping("/dhobis/{userId}/unban")
    public void unbanDhobi(@PathVariable Long userId) {
        dhobiService.unban(userId);
    }

    // ===== Reviews =====

    /** Every review in the system — for the admin's reports/reviews view. */
    @GetMapping("/reviews")
    public List<ReviewResponse> allReviews() {
        return reviewService.getAll();
    }

    // ===== Catalog management — "admin adds new features/items" =====

    @PostMapping("/catalog")
    public CatalogItemResponse createCatalogItem(@Valid @RequestBody CatalogItemRequest req) {
        return catalogService.create(req);
    }

    @PutMapping("/catalog/{id}")
    public CatalogItemResponse updateCatalogItem(@PathVariable Long id, @Valid @RequestBody CatalogItemRequest req) {
        return catalogService.update(id, req);
    }

    @DeleteMapping("/catalog/{id}")
    public void deleteCatalogItem(@PathVariable Long id) {
        catalogService.delete(id);
    }

    // ===== helpers =====

    private UserSummary toSummary(User u) {
        return new UserSummary(u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.isEnabled(), u.getAccountType(), u.getCompanyName());
    }

    public record UserSummary(Long id, String fullName, String email, String phone, boolean enabled, String accountType, String companyName) {}
}
