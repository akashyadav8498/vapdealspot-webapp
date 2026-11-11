package com.vapedealspot.auth;

import com.vapedealspot.user.model.User;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.HexFormat;

@Service
public class PasswordService {

    public static final int PBKDF2_ITERATIONS = 600_000;
    public static final int PBKDF2_KEY_LENGTH_BITS = 256;
    public static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";

    private final SecureRandom secureRandom = new SecureRandom();

    // =========================================================================
    // UNIFIED ARCHITECTURE: SINGLE ENCODED PASSWORD VERIFICATION & ENCODING
    // =========================================================================

    /**
     * Verifies raw password against the single self-describing encoded password column on a unified User.
     * Supports both modular PBKDF2 (pbkdf2_sha256$600000$...) and BCrypt ($2a$, $2b$, $2y$).
     */
    public boolean verifyPassword(String rawPassword, User user) {
        if (rawPassword == null || user == null || user.getPassword() == null) {
            return false;
        }
        return verifyEncodedPassword(rawPassword, user.getPassword());
    }

    /**
     * Verifies raw password against a self-describing encoded password string.
     * Safely rejects null, empty, blank, malformed, or unsupported formats.
     */
    public boolean verifyEncodedPassword(String rawPassword, String encodedPassword) {
        if (rawPassword == null || rawPassword.isEmpty() || encodedPassword == null || encodedPassword.isEmpty()) {
            return false;
        }

        // BCrypt formats ($2a$, $2b$, $2y$)
        if (encodedPassword.startsWith("$2a$") || encodedPassword.startsWith("$2b$") || encodedPassword.startsWith("$2y$")) {
            return verifyBcrypt(rawPassword, encodedPassword);
        }

        // Modular self-describing PBKDF2 format (pbkdf2_sha256$<iterations>$<salt>$<hash>)
        if (encodedPassword.startsWith("pbkdf2_sha256$")) {
            return verifyPbkdf2Encoded(rawPassword, encodedPassword);
        }

        return false;
    }

    /**
     * Verifies raw password against a modular self-describing PBKDF2 encoded string.
     * Format: pbkdf2_sha256$<iterations>$<salt_hex>$<hash_hex>
     */
    public boolean verifyPbkdf2Encoded(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null || !encodedPassword.startsWith("pbkdf2_sha256$")) {
            return false;
        }
        String[] parts = encodedPassword.split("\\$");
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations <= 0 || iterations > 2_000_000) {
                return false;
            }
            String salt = parts[2];
            String expectedHexHash = parts[3];
            if (salt.isEmpty() || expectedHexHash.isEmpty()) {
                return false;
            }

            PBEKeySpec spec = new PBEKeySpec(
                rawPassword.toCharArray(),
                salt.getBytes(StandardCharsets.UTF_8),
                iterations,
                PBKDF2_KEY_LENGTH_BITS
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            String computedHex = HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(
                computedHex.getBytes(StandardCharsets.UTF_8),
                expectedHexHash.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            // Safely reject any malformed numeric, encoding, or parsing input
            return false;
        }
    }

    /**
     * Encodes a new password using BCrypt with cost factor 12.
     * Used for newly registered customer accounts.
     */
    public String encodePassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty");
        }
        return hashBcrypt(rawPassword);
    }

    // =========================================================================
    // LEGACY COMPATIBILITY: ADMIN USER AUTHENTICATION & ADMIN USER SERVICE
    // =========================================================================

    /**
     * Verifies raw password against a BCrypt hash.
     */
    public boolean verifyBcrypt(String rawPassword, String expectedBcryptHash) {
        if (rawPassword == null || expectedBcryptHash == null || expectedBcryptHash.isEmpty()) {
            return false;
        }
        try {
            return BCrypt.checkpw(rawPassword, expectedBcryptHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Computes PBKDF2-HMAC-SHA256 hash using 600,000 iterations.
     */
    public String hashPbkdf2(String rawPassword, String salt) {
        if (rawPassword == null || salt == null) {
            throw new IllegalArgumentException("Password and salt must not be null");
        }
        try {
            PBEKeySpec spec = new PBEKeySpec(
                rawPassword.toCharArray(),
                salt.getBytes(StandardCharsets.UTF_8),
                PBKDF2_ITERATIONS,
                PBKDF2_KEY_LENGTH_BITS
            );
            SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] hash = skf.generateSecret(spec).getEncoded();
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Failed to calculate PBKDF2 hash", e);
        }
    }

    /**
     * Constant-time comparison between raw password and expected PBKDF2 hex hash.
     */
    public boolean verifyPbkdf2(String rawPassword, String salt, String expectedHexHash) {
        if (rawPassword == null || salt == null || expectedHexHash == null) {
            return false;
        }
        String computedHash = hashPbkdf2(rawPassword, salt);
        return MessageDigest.isEqual(
            computedHash.getBytes(StandardCharsets.UTF_8),
            expectedHexHash.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Generates a cryptographically secure hex salt.
     */
    public String generateSalt(int numBytes) {
        byte[] salt = new byte[numBytes];
        secureRandom.nextBytes(salt);
        return HexFormat.of().formatHex(salt);
    }

    public record HashedPassword(String passwordHash, String salt) {}

    /**
     * Hashes a new password using PBKDF2-HMAC-SHA256 with a freshly generated random salt.
     */
    public HashedPassword hashNewPassword(String rawPassword) {
        String salt = generateSalt(16);
        String hash = hashPbkdf2(rawPassword, salt);
        return new HashedPassword(hash, salt);
    }

    public String generateSalt() {
        return generateSalt(16);
    }

    /**
     * Hashes password using BCrypt with cost factor 12.
     */
    public String hashBcrypt(String rawPassword) {
        return BCrypt.hashpw(rawPassword, BCrypt.gensalt(12, secureRandom));
    }
}
