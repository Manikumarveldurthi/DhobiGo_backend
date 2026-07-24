package com.dhobigo.backend.dto;

import com.dhobigo.backend.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    /**
     * Public signup only allows CUSTOMER or DHOBI — ADMIN accounts are
     * created by another admin (see AdminController), never self-registered.
     */
    public record RegisterRequest(
            @NotBlank String fullName,
            @Email @NotBlank String email,
            @NotBlank String phone,
            @Size(min = 8, message = "Password must be at least 8 characters") String password,
            @NotBlank String role, // "CUSTOMER" or "DHOBI"
            // Optional — someone else's referral code, credits both wallets on success (see AuthService)
            String referralCode,
            // Optional — "CORPORATE" for hostel/hotel/bulk accounts, defaults to "INDIVIDUAL"
            String accountType,
            String companyName
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record GoogleLoginRequest(
            @NotBlank String idToken
    ) {}

    public record SendOtpRequest(
            @NotBlank String phone
    ) {}

    public record VerifyOtpRequest(
            @NotBlank String phone,
            @NotBlank String code,
            String fullName // only used if this phone number is signing up for the first time
    ) {}

    public record ForgotPasswordRequest(
            @Email @NotBlank String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank String token,
            @Size(min = 8, message = "Password must be at least 8 characters") String newPassword
    ) {}

    public record AuthResponse(
            String token,
            Long userId,
            String fullName,
            String email,
            Role role
    ) {}
}
