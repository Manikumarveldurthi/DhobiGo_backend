package com.dhobigo.backend.dto;

import com.dhobigo.backend.model.Role;

public class UserDtos {

    public record ProfileResponse(
            Long id,
            String fullName,
            String email,
            String phone,
            String address,
            String avatarUrl,
            Role role,
            String referralCode,
            int walletBalance,
            String loyaltyTier,        // "BRONZE" | "SILVER" | "GOLD" — customers only, null otherwise
            String accountType,        // "INDIVIDUAL" | "CORPORATE"
            String companyName
    ) {}

    public record UpdateProfileRequest(
            String fullName,
            String email,
            String phone,
            String address,
            String avatarUrl,
            String companyName
    ) {}
    
    public record ChangePasswordRequest(
            @jakarta.validation.constraints.NotBlank String currentPassword,
            @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(min = 8, message = "New password must be at least 8 characters") String newPassword
    ) {}
}
