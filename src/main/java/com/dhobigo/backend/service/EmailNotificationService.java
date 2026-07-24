package com.dhobigo.backend.service;

import com.dhobigo.backend.config.EmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final EmailProperties emailProperties;
    // ObjectProvider, not a direct JavaMailSender injection — spring.mail.*
    // may not be configured at all, and we don't want startup to fail just
    // because email isn't set up yet.
    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    public EmailNotificationService(EmailProperties emailProperties, ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.emailProperties = emailProperties;
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String resetLink = emailProperties.getResetLinkBase() + "?token=" + resetToken;

        if (!emailProperties.isEnabled()) {
            log.info("[Email DISABLED — would send] to={} resetLink={}", toEmail, resetLink);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Email is enabled but no mail sender is configured (check SPRING_MAIL_* env vars). Reset link for {}: {}", toEmail, resetLink);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailProperties.getFrom());
            message.setTo(toEmail);
            message.setSubject("Reset your DhobiGo password");
            message.setText(
                    "We received a request to reset your DhobiGo password.\n\n" +
                    "Click this link to set a new one (expires in 30 minutes):\n" +
                    resetLink + "\n\n" +
                    "If you didn't request this, you can safely ignore this email."
            );
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
