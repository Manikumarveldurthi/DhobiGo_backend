package com.dhobigo.backend.service;

import com.dhobigo.backend.dto.DhobiDtos.DhobiResponse;
import com.dhobigo.backend.dto.DhobiDtos.PendingDhobiResponse;
import com.dhobigo.backend.exception.ApiException;
import com.dhobigo.backend.model.DhobiProfile;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.repository.DhobiProfileRepository;
import com.dhobigo.backend.repository.UserRepository;
import com.dhobigo.backend.util.GeoUtil;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class DhobiService {

    private final DhobiProfileRepository dhobiProfileRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public DhobiService(DhobiProfileRepository dhobiProfileRepository, UserRepository userRepository, SimpMessagingTemplate messagingTemplate) {
        this.dhobiProfileRepository = dhobiProfileRepository;
        this.userRepository = userRepository;
        this.messagingTemplate = messagingTemplate;
    }

    /**
     * Live dhobis for customers to browse. If lat/lng are supplied, results
     * are sorted nearest-first and include distanceKm — same idea as
     * Swiggy/Zomato showing "X km away" sorted by proximity.
     */
    public List<DhobiResponse> getAvailableDhobis(Double customerLat, Double customerLng) {
        List<DhobiProfile> profiles = dhobiProfileRepository.findByApprovedTrueAndAvailableTrue();

        List<DhobiResponse> responses = new ArrayList<>();
        for (DhobiProfile p : profiles) {
            Double distance = null;
            if (customerLat != null && customerLng != null && p.getLatitude() != null && p.getLongitude() != null) {
                distance = GeoUtil.distanceKm(customerLat, customerLng, p.getLatitude(), p.getLongitude());
            }
            responses.add(toResponse(p, distance));
        }

        if (customerLat != null && customerLng != null) {
            responses.sort(Comparator.comparing(
                    DhobiResponse::distanceKm,
                    Comparator.nullsLast(Comparator.naturalOrder())
            ));
        }
        return responses;
    }

    public List<PendingDhobiResponse> getPendingApprovals() {
        return dhobiProfileRepository.findByApprovedFalse().stream()
                .map(p -> new PendingDhobiResponse(
                        p.getUser().getId(),
                        p.getUser().getFullName(),
                        p.getUser().getEmail(),
                        p.getUser().getPhone(),
                        p.getUser().getCreatedAt().toString()
                ))
                .toList();
    }

    @Transactional
    public void approve(Long userId) {
        DhobiProfile profile = findProfileOrThrow(userId);
        profile.setApproved(true);
        profile.setAvailable(true); // go live immediately on approval
        profile.setApprovedAt(Instant.now());
        dhobiProfileRepository.save(profile);
    }

    @Transactional
    public void reject(Long userId) {
        DhobiProfile profile = findProfileOrThrow(userId);
        profile.setApproved(false);
        profile.setAvailable(false);
        dhobiProfileRepository.save(profile);
        // Note: the User account itself is untouched — they can be
        // reconsidered later. To permanently block them instead, disable
        // their User.enabled flag via the admin users endpoint.
    }

    @Transactional
    public void updateLocation(User dhobi, double lat, double lng) {
        DhobiProfile profile = findProfileOrThrow(dhobi.getId());
        profile.setLatitude(lat);
        profile.setLongitude(lng);
        profile.setLocationUpdatedAt(Instant.now());
        dhobiProfileRepository.save(profile);

        messagingTemplate.convertAndSend(
                "/topic/dhobis/" + dhobi.getId() + "/location",
                Map.of("latitude", lat, "longitude", lng, "updatedAt", Instant.now().toString())
        );
    }

    /** Lets dhobi.html check "am I approved yet?" right after login. */
    public DhobiResponse getMyProfile(User dhobi) {
        DhobiProfile profile = findProfileOrThrow(dhobi.getId());
        return toResponse(profile, null);
    }

    @Transactional
    public void setAvailability(Long userId, boolean available) {
        DhobiProfile profile = findProfileOrThrow(userId);
        if (available && !profile.getApproved()) {
            throw new ApiException("Cannot go available before admin approval", HttpStatus.FORBIDDEN);
        }
        profile.setAvailable(available);
        dhobiProfileRepository.save(profile);
    }

    @Transactional
    public void ban(Long userId) {
        DhobiProfile profile = findProfileOrThrow(userId);
        profile.setApproved(false);
        profile.setAvailable(false);
        dhobiProfileRepository.save(profile);

        User user = profile.getUser();
        user.setEnabled(false); // blocks login entirely, not just delisting
        userRepository.save(user);
    }

    @Transactional
    public void unban(Long userId) {
        DhobiProfile profile = findProfileOrThrow(userId);
        User user = profile.getUser();
        user.setEnabled(true);
        userRepository.save(user);
        // Deliberately does NOT restore approved/available — admin should
        // re-review and re-approve explicitly via the normal approval flow,
        // same as a brand-new signup.
    }

    // ===== helpers =====

    private DhobiProfile findProfileOrThrow(Long userId) {
        return dhobiProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ApiException("Dhobi profile not found", HttpStatus.NOT_FOUND));
    }

    private DhobiResponse toResponse(DhobiProfile p, Double distanceKm) {
        return new DhobiResponse(
                p.getUser().getId(),
                p.getUser().getFullName(),
                p.getRating(),
                p.getCompletedOrders(),
                p.getAvailable(),
                p.getApproved(),
                computeBadges(p),
                distanceKm,
                null, // phone hidden here — see AdminController for the admin view that includes it
                p.getUser().isEnabled()
        );
    }

    /**
     * Simple tiered badges, computed on the fly (not stored) so they always
     * reflect current stats. Mirrors the kind of trust signals Swiggy/Zomato
     * show on delivery partners/restaurants.
     */
    private List<String> computeBadges(DhobiProfile p) {
        List<String> badges = new ArrayList<>();

        if (p.getRating() != null && p.getRating() >= 4.8) {
            badges.add("Top Rated");
        }
        if (p.getCompletedOrders() != null) {
            if (p.getCompletedOrders() >= 500) {
                badges.add("500+ Orders");
            } else if (p.getCompletedOrders() >= 100) {
                badges.add("100+ Orders");
            } else if (p.getCompletedOrders() < 10) {
                badges.add("New");
            }
        }
        if (Boolean.TRUE.equals(p.getAvailable())) {
            badges.add("Online Now");
        }
        return badges;
    }
}
