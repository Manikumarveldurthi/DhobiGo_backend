package com.dhobigo.backend.controller;

import com.dhobigo.backend.dto.AuthDtos.*;
import com.dhobigo.backend.service.AuthService;
import com.dhobigo.backend.service.GoogleAuthService;
import com.dhobigo.backend.service.PhoneAuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final PhoneAuthService phoneAuthService;

    public AuthController(AuthService authService, GoogleAuthService googleAuthService, PhoneAuthService phoneAuthService) {
        this.authService = authService;
        this.googleAuthService = googleAuthService;
        this.phoneAuthService = phoneAuthService;
    }

    /** Public signup — role must be CUSTOMER or DHOBI (never ADMIN). */
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    /** Google Sign-In — see GoogleAuthService for setup steps. Logs in if the email exists, auto-registers as CUSTOMER otherwise. */
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest req) {
        return ResponseEntity.ok(googleAuthService.loginOrRegister(req.idToken()));
    }

    /** Step 1 of phone login — sends an SMS OTP via Twilio Verify. */
    @PostMapping("/phone/send-otp")
    public ResponseEntity<Void> sendOtp(@Valid @RequestBody SendOtpRequest req) {
        phoneAuthService.sendOtp(req.phone());
        return ResponseEntity.ok().build();
    }

    /** Step 2 — verifies the code, logs in if the phone exists, auto-registers as CUSTOMER otherwise (needs fullName on first signup). */
    @PostMapping("/phone/verify-otp")
    public ResponseEntity<AuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return ResponseEntity.ok(phoneAuthService.verifyOtp(req.phone(), req.code(), req.fullName()));
    }

    /** Always returns 200 regardless of whether the email exists — see AuthService.forgotPassword for why. */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.token(), req.newPassword());
        return ResponseEntity.ok().build();
    }
}
