package com.dhobigo.backend.util;

import com.dhobigo.backend.repository.UserRepository;

import java.security.SecureRandom;

/** Generates a short, unique "DGxxxxxx" referral code for a new user. */
public class ReferralCodeUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private ReferralCodeUtil() {
    }

    public static String generate(UserRepository userRepository) {
        String code;
        do {
            code = "DG" + (100000 + RANDOM.nextInt(900000));
        } while (userRepository.existsByReferralCode(code));
        return code;
    }
}
