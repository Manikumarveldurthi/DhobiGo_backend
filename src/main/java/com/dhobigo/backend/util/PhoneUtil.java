package com.dhobigo.backend.util;

/**
 * Normalizes phone numbers so the SAME real number always matches in the
 * database no matter which screen it was typed into (regular signup,
 * phone-OTP login/signup, profile edit) or whether the person included
 * the country code.
 *
 * Without this, "9876543210" (typed on the regular signup form) and
 * "+919876543210" (typed on the phone-OTP login, since Twilio Verify
 * requires the country code) are treated as two different numbers —
 * so an existing user trying phone login looks like a brand-new signup.
 */
public class PhoneUtil {

    private PhoneUtil() {
    }

    public static String normalize(String rawPhone, String defaultCountryCode) {
        if (rawPhone == null) return null;
        String cleaned = rawPhone.trim().replaceAll("[\\s\\-()]", "");
        if (cleaned.isEmpty() || cleaned.startsWith("+")) return cleaned;
        return defaultCountryCode + cleaned;
    }
}
