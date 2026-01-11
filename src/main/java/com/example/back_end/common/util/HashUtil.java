package com.example.back_end.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utility class for hashing operations
 */
public class HashUtil {

    private static final String ALGORITHM = "SHA-256";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Hash a string using SHA-256
     * @param input String to hash
     * @return Base64 encoded hash
     */
    public static String hashString(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash string", e);
        }
    }

    /**
     * Generate a random pairing code (6 digits)
     * @return 6-digit pairing code
     */
    public static String generatePairingCode() {
        int code = 100000 + SECURE_RANDOM.nextInt(900000);
        return String.valueOf(code);
    }

    /**
     * Generate a random browser token (UUID-like)
     * @return Random token string
     */
    public static String generateBrowserToken() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    /**
     * Verify if a plain code matches a hash
     * @param plainCode Plain code
     * @param hash Hash to compare against
     * @return true if matches
     */
    public static boolean verifyHash(String plainCode, String hash) {
        String computedHash = hashString(plainCode);
        return computedHash.equals(hash);
    }
}