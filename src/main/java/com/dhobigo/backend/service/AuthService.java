package com.dhobigo.backend.service;

import com.dhobigo.backend.config.WhatsAppProperties;
import com.dhobigo.backend.dto.AuthDtos.AuthResponse;
import com.dhobigo.backend.dto.AuthDtos.LoginRequest;
import com.dhobigo.backend.dto.AuthDtos.RegisterRequest;
import com.dhobigo.backend.exception.ApiException;
import com.dhobigo.backend.model.DhobiProfile;
import com.dhobigo.backend.model.PasswordResetToken;
import com.dhobigo.backend.model.Role;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.repository.DhobiProfileRepository;
import com.dhobigo.backend.repository.PasswordResetTokenRepository;
import com.dhobigo.backend.repository.UserRepository;
import com.dhobigo.backend.security.JwtService;
import com.dhobigo.backend.util.PhoneUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    /** Rupees credited to BOTH the new signup and their referrer, when a valid referral code is used. */
    private static final int REFERRAL_BONUS = 50;

    private final UserRepository userRepository;
    private final DhobiProfileRepository dhobiProfileRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final EmailNotificationService emailNotificationService;
    private final WhatsAppProperties phoneProps; // reused only for its defaultCountryCode, same as PhoneAuthService
    private final WalletService walletService;

    public AuthService(UserRepository userRepository,
                        DhobiProfileRepository dhobiProfileRepository,
                        PasswordResetTokenRepository passwordResetTokenRepository,
                        PasswordEncoder passwordEncoder,
                        AuthenticationManager authenticationManager,
                        JwtService jwtService,
                        EmailNotificationService emailNotificationService,
                        WhatsAppProperties phoneProps,
                        WalletService walletService) {
        this.userRepository = userRepository;
        this.dhobiProfileRepository = dhobiProfileRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.emailNotificationService = emailNotificationService;
        this.phoneProps = phoneProps;
        this.walletService = walletService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.email())) {
            throw new ApiException("An account with this email already exists", HttpStatus.CONFLICT);
        }

        // Check both the normalized form and the raw-as-typed form, so this
        // also catches accounts whose phone was saved before normalization
        // was added (see PhoneUtil).
        String normalizedPhone = PhoneUtil.normalize(req.phone(), phoneProps.getDefaultCountryCode());
        if (userRepository.existsByPhone(normalizedPhone) || userRepository.existsByPhone(req.phone().trim())) {
            throw new ApiException("An account with this phone number already exists — please log in instead", HttpStatus.CONFLICT);
        }

        Role role;
        try {
            role = Role.valueOf(req.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ApiException("Role must be CUSTOMER or DHOBI", HttpStatus.BAD_REQUEST);
        }
        if (role == Role.ADMIN) {
            // Admin accounts are never created through public signup.
            throw new ApiException("Cannot self-register as ADMIN", HttpStatus.FORBIDDEN);
        }

        User user = User.builder()
                .fullName(req.fullName())
                .email(req.email())
                .phone(normalizedPhone)
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(role)
                .enabled(true)
                .referralCode(com.dhobigo.backend.util.ReferralCodeUtil.generate(userRepository))
                .referredByCode(req.referralCode())
                .accountType((req.accountType() != null && !req.accountType().isBlank()) ? req.accountType().toUpperCase() : "INDIVIDUAL")
                .companyName(req.companyName())
                .build();

        final User savedUser = userRepository.save(user);

        // Referral bonus: both sides get credited once, only if the code
        // given actually belongs to someone (silently ignored if not —
        // a typo in this optional field shouldn't block signup).
        if (req.referralCode() != null && !req.referralCode().isBlank()) {
            userRepository.findByReferralCode(req.referralCode().trim()).ifPresent(referrer -> {
                if (!referrer.getId().equals(user.getId())) {
                    walletService.credit(referrer, REFERRAL_BONUS, com.dhobigo.backend.model.WalletTransactionType.REFERRAL_BONUS,
                            "Referral bonus — " + user.getFullName() + " signed up with your code");
                    walletService.credit(user, REFERRAL_BONUS, com.dhobigo.backend.model.WalletTransactionType.REFERRAL_BONUS,
                            "Welcome bonus for using a referral code");
                }
            });
        }

        if (role == Role.DHOBI) {
            DhobiProfile profile = DhobiProfile.builder()
                    .user(user)
                    .build();
            dhobiProfileRepository.save(profile);
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    public AuthResponse login(LoginRequest req) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );

        User user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new ApiException("Invalid email or password", HttpStatus.UNAUTHORIZED));

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    /**
     * Always succeeds from the caller's point of view, whether or not the
     * email exists — this is deliberate: telling the caller "no account
     * with that email" would let anyone probe which emails are registered.
     * The real email only gets sent if the account exists.
     */
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                    .build();
            passwordResetTokenRepository.save(resetToken);
            emailNotificationService.sendPasswordResetEmail(email, token);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ApiException("Invalid or expired reset link", HttpStatus.BAD_REQUEST));

        if (!resetToken.isValid()) {
            throw new ApiException("This reset link has expired or was already used", HttpStatus.BAD_REQUEST);
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }
}
