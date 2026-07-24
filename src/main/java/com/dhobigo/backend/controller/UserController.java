package com.dhobigo.backend.controller;

import com.dhobigo.backend.config.WhatsAppProperties;
import com.dhobigo.backend.dto.UserDtos.ProfileResponse;
import com.dhobigo.backend.dto.UserDtos.UpdateProfileRequest;
import com.dhobigo.backend.exception.ApiException;
import com.dhobigo.backend.model.OrderStage;
import com.dhobigo.backend.model.Role;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.repository.OrderRepository;
import com.dhobigo.backend.repository.UserRepository;
import com.dhobigo.backend.util.PhoneUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Works for any logged-in role — customer, dhobi, or admin editing their own account. */
@RestController
@RequestMapping("/api/users/me")
public class UserController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final WhatsAppProperties phoneProps; // reused only for its defaultCountryCode
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public UserController(UserRepository userRepository, OrderRepository orderRepository, WhatsAppProperties phoneProps,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
this.userRepository = userRepository;
this.orderRepository = orderRepository;
this.phoneProps = phoneProps;
this.passwordEncoder = passwordEncoder;
}

    @GetMapping
    public ProfileResponse getMyProfile(@AuthenticationPrincipal User user) {
        return toResponse(user);
    }

    @PatchMapping
    public ProfileResponse updateMyProfile(@AuthenticationPrincipal User user, @RequestBody UpdateProfileRequest req) {
        if (req.fullName() != null && !req.fullName().isBlank()) user.setFullName(req.fullName());

        // Phone-OTP and Google sign-ups start with no email — this is the
        // one-time path to add it. Once an email is set, it stays locked
        // here (same as every other account) so this can't be used to
        // silently move an account to a different address later.
        if (req.email() != null && !req.email().isBlank()) {
            String newEmail = req.email().trim();
            if (user.getEmail() != null && !user.getEmail().equalsIgnoreCase(newEmail)) {
                throw new ApiException("Email is already set for this account", HttpStatus.CONFLICT);
            }
            if (user.getEmail() == null) {
                userRepository.findByEmail(newEmail)
                        .filter(other -> !other.getId().equals(user.getId()))
                        .ifPresent(other -> {
                            throw new ApiException("That email is already used by another account", HttpStatus.CONFLICT);
                        });
                user.setEmail(newEmail);
            }
        }

        if (req.phone() != null) {
            String normalizedPhone = PhoneUtil.normalize(req.phone(), phoneProps.getDefaultCountryCode());
            boolean changingNumber = !normalizedPhone.equals(user.getPhone());
            if (changingNumber && userRepository.findByPhone(normalizedPhone).filter(other -> !other.getId().equals(user.getId())).isPresent()) {
                throw new ApiException("That phone number is already used by another account", HttpStatus.CONFLICT);
            }
            user.setPhone(normalizedPhone);
        }
        if (req.address() != null) user.setAddress(req.address());
        if (req.avatarUrl() != null) user.setAvatarUrl(req.avatarUrl());
        if (req.companyName() != null) user.setCompanyName(req.companyName());
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    private ProfileResponse toResponse(User u) {
    	// Existing accounts created before referral codes existed have a null
        // one — generate and persist it the first time it's ever read, instead
        // of only generating it at signup.
        if (u.getReferralCode() == null) {
            u.setReferralCode(com.dhobigo.backend.util.ReferralCodeUtil.generate(userRepository));
            u = userRepository.save(u);
        }

        String loyaltyTier = null;
        if (u.getRole() == Role.CUSTOMER) {
            long delivered = orderRepository.countByCustomerIdAndStage(u.getId(), OrderStage.DELIVERED);
            loyaltyTier = delivered >= 15 ? "GOLD" : delivered >= 5 ? "SILVER" : "BRONZE";
        }
        return new ProfileResponse(
                u.getId(), u.getFullName(), u.getEmail(), u.getPhone(), u.getAddress(), u.getAvatarUrl(), u.getRole(),
                u.getReferralCode(), u.getWalletBalance(), loyaltyTier, u.getAccountType(), u.getCompanyName()
        );
    }
    
    @PatchMapping("/password")
    public void changePassword(@AuthenticationPrincipal User user, @jakarta.validation.Valid @RequestBody com.dhobigo.backend.dto.UserDtos.ChangePasswordRequest req) {
        if (!passwordEncoder.matches(req.currentPassword(), user.getPasswordHash())) {
            throw new ApiException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
    }
}
