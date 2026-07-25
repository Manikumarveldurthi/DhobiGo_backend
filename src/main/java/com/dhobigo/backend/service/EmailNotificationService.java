package com.dhobigo.backend.service;

import com.dhobigo.backend.config.EmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * Sends the forgot-password reset link by email via the Resend HTTP API.
 *
 * Uses HTTPS (port 443), not SMTP — this matters because platforms like
 * Render block outbound SMTP ports (25/465/587) on their free tier, which
 * is why a JavaMailSender/Gmail-SMTP approach times out once deployed even
 * though it works fine locally. See EmailProperties for setup steps.
 */
@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);
    private static final String RESEND_API_URL = "https://api.resend.com/emails";

    private final EmailProperties emailProperties;
    private final RestTemplate restTemplate = new RestTemplate();

    public EmailNotificationService(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = emailProperties.getResetLinkBase() + "?token=" + resetToken;

        if (!emailProperties.isEnabled()) {
            log.info("[Email DISABLED — would send] to={} resetLink={}", toEmail, resetLink);
            return;
        }

        String apiKey = emailProperties.getResendApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_RESEND_API_KEY")) {
            log.warn("Email is enabled but RESEND_API_KEY is not set. Reset link for {}: {}", toEmail, resetLink);
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            String text =
                    "We received a request to reset your DhobiGo password.\n\n" +
                    "Click this link to set a new one (expires in 30 minutes):\n" +
                    resetLink + "\n\n" +
                    "If you didn't request this, you can safely ignore this email.";

            Map<String, Object> body = Map.of(
                    "from", emailProperties.getFrom(),
                    "to", List.of(toEmail),
                    "subject", "Reset your DhobiGo password",
                    "text", text
            );

            restTemplate.postForEntity(RESEND_API_URL, new HttpEntity<>(body, headers), String.class);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
