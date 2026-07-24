package com.dhobigo.backend.service;

import com.dhobigo.backend.config.GoogleAuthProperties;
import com.dhobigo.backend.dto.AuthDtos.AuthResponse;
import com.dhobigo.backend.exception.ApiException;
import com.dhobigo.backend.model.Role;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.repository.UserRepository;
import com.dhobigo.backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

/**
 * Verifies a Google ID token (obtained client-side via Google Identity
 * Services, see js/login.js) using Google's own tokeninfo endpoint —
 * simplest possible verification path, no extra library needed. Google
 * signs these tokens; calling their endpoint confirms the signature and
 * hands back the decoded claims in one step.
 */
@Service
public class GoogleAuthService {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final GoogleAuthProperties googleAuthProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RestTemplate restTemplate = new RestTemplate();

    public GoogleAuthService(GoogleAuthProperties googleAuthProperties,
                              UserRepository userRepository,
                              PasswordEncoder passwordEncoder,
                              JwtService jwtService) {
        this.googleAuthProperties = googleAuthProperties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse loginOrRegister(String idToken) {
        if (googleAuthProperties.getClientId() == null || googleAuthProperties.getClientId().isBlank()) {
            throw new ApiException(
                    "Google Sign-In isn't configured yet — set GOOGLE_CLIENT_ID on the backend",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        Map<String, Object> claims;
        try {
            claims = restTemplate.getForObject(TOKENINFO_URL + idToken, Map.class);
        } catch (Exception e) {
            throw new ApiException("Invalid Google token", HttpStatus.UNAUTHORIZED);
        }
        if (claims == null) {
            throw new ApiException("Invalid Google token", HttpStatus.UNAUTHORIZED);
        }

        String audience = String.valueOf(claims.get("aud"));
        if (!googleAuthProperties.getClientId().equals(audience)) {
            throw new ApiException("Token was not issued for this app", HttpStatus.UNAUTHORIZED);
        }

        String emailVerified = String.valueOf(claims.get("email_verified"));
        if (!"true".equals(emailVerified)) {
            throw new ApiException("Google account email is not verified", HttpStatus.UNAUTHORIZED);
        }

        String email = (String) claims.get("email");
        String name = (String) claims.getOrDefault("name", email);

        User user = userRepository.findByEmail(email).orElseGet(() -> registerGoogleUser(email, name));

        if (!user.isEnabled()) {
            throw new ApiException("This account has been disabled", HttpStatus.FORBIDDEN);
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    private User registerGoogleUser(String email, String name) {
        // Google accounts never use password login — generate a random,
        // never-shown password hash so the passwordHash column stays
        // non-null-friendly for existing password-login code paths.
        String randomPassword = Base64.getEncoder().encodeToString(
                new SecureRandom().generateSeed(24)
        );

        User user = User.builder()
                .fullName(name)
                .email(email)
                .passwordHash(passwordEncoder.encode(randomPassword))
                .role(Role.CUSTOMER) // Google quick-signup is customer-only, same as Swiggy/Zomato's social login
                .enabled(true)
                .referralCode(com.dhobigo.backend.util.ReferralCodeUtil.generate(userRepository))
                .build();
        return userRepository.save(user);
    }
}
