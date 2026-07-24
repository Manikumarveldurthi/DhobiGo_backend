package com.dhobigo.backend.service;

import com.dhobigo.backend.config.TwilioVerifyProperties;
import com.dhobigo.backend.config.WhatsAppProperties;
import com.dhobigo.backend.dto.AuthDtos.AuthResponse;
import com.dhobigo.backend.exception.ApiException;
import com.dhobigo.backend.model.Role;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.repository.UserRepository;
import com.dhobigo.backend.security.JwtService;
import com.dhobigo.backend.util.PhoneUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@Service
public class PhoneAuthService {

    private static final Logger log = LoggerFactory.getLogger(PhoneAuthService.class);
    private static final String VERIFY_START_URL = "https://verify.twilio.com/v2/Services/%s/Verifications";
    private static final String VERIFY_CHECK_URL = "https://verify.twilio.com/v2/Services/%s/VerificationCheck";

    private final TwilioVerifyProperties verifyProps;
    private final WhatsAppProperties twilioAccountProps; // reused for Account SID / Auth Token
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RestTemplate restTemplate = new RestTemplate();

    public PhoneAuthService(TwilioVerifyProperties verifyProps,
                             WhatsAppProperties twilioAccountProps,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             JwtService jwtService) {
        this.verifyProps = verifyProps;
        this.twilioAccountProps = twilioAccountProps;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void sendOtp(String phone) {
        String normalizedPhone = PhoneUtil.normalize(phone, twilioAccountProps.getDefaultCountryCode());

        if (!verifyProps.isEnabled()) {
            log.info("[Phone OTP DISABLED — would send] to={}", normalizedPhone);
            return; // stays quiet on the client side too — see AuthController, always returns generic success
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(twilioAccountProps.getAccountSid(), twilioAccountProps.getAuthToken());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", normalizedPhone);
        body.add("Channel", "sms");

        String url = String.format(VERIFY_START_URL, verifyProps.getServiceSid());
        try {
            restTemplate.postForEntity(url, new HttpEntity<>(body, headers), String.class);
        } catch (Exception e) {
            log.warn("Failed to send OTP to {}: {}", normalizedPhone, e.getMessage());
            throw new ApiException("Couldn't send verification code — check the phone number and try again", org.springframework.http.HttpStatus.BAD_GATEWAY);
        }
    }

    @Transactional
    public AuthResponse verifyOtp(String phone, String code, String fullNameIfNew) {
        if (!verifyProps.isEnabled()) {
            throw new ApiException(
                    "Phone login isn't configured yet — set TWILIO_VERIFY_ENABLED and TWILIO_VERIFY_SERVICE_SID on the backend",
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
            );
        }

        String normalizedPhone = PhoneUtil.normalize(phone, twilioAccountProps.getDefaultCountryCode());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(twilioAccountProps.getAccountSid(), twilioAccountProps.getAuthToken());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("To", normalizedPhone);
        body.add("Code", code);

        String url = String.format(VERIFY_CHECK_URL, verifyProps.getServiceSid());
        Map<?, ?> result;
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, new HttpEntity<>(body, headers), Map.class);
            result = response.getBody();
        } catch (Exception e) {
            throw new ApiException("Verification failed — the code may be wrong or expired", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }

        if (result == null || !"approved".equals(result.get("status"))) {
            throw new ApiException("Incorrect or expired code", org.springframework.http.HttpStatus.UNAUTHORIZED);
        }

        // Look up by the normalized number first (how every new/edited
        // record is stored now); fall back to whatever raw string the
        // person typed, in case this account's phone predates this fix
        // and is still saved un-normalized in the database.
        User user = userRepository.findByPhone(normalizedPhone)
                .or(() -> userRepository.findByPhone(phone.trim()))
                .orElseGet(() -> registerPhoneUser(normalizedPhone, fullNameIfNew));

        if (!user.isEnabled()) {
            throw new ApiException("This account has been disabled", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getId(), user.getFullName(), user.getEmail(), user.getRole());
    }

    private User registerPhoneUser(String phone, String fullName) {
        if (fullName == null || fullName.isBlank()) {
            throw new ApiException("First-time phone login needs your name", org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        String randomPassword = Base64.getEncoder().encodeToString(new SecureRandom().generateSeed(24));

        User user = User.builder()
                .fullName(fullName)
                .phone(phone)
                .passwordHash(passwordEncoder.encode(randomPassword))
                .role(Role.CUSTOMER) // phone quick-signup is customer-only, same reasoning as Google
                .enabled(true)
                .referralCode(com.dhobigo.backend.util.ReferralCodeUtil.generate(userRepository))
                .build();
        return userRepository.save(user);
    }
}
