package com.ems.security.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Keyed HMAC-SHA256 of a sensitive value. Unlike the random-IV ciphertext, the hash is
 * deterministic, so a UNIQUE index on it prevents duplicate Aadhaar/PAN numbers.
 * A secret key (not a plain SHA-256) stops anyone with DB access from brute-forcing
 * the 12-digit Aadhaar space.
 */
@Component
public class SensitiveDataHasher {

    private static final String ALGORITHM = "HmacSHA256";

    private final SecretKeySpec key;

    public SensitiveDataHasher(@Value("${app.security.hash-key}") String base64Key) {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("app.security.hash-key must be a Base64-encoded key of at least 32 bytes");
        }
        this.key = new SecretKeySpec(keyBytes, ALGORITHM);
    }

    /** Returns 64 lowercase hex characters. */
    public String hash(String value) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM); // Mac is not thread-safe, so one per call
            mac.init(key);
            return HexFormat.of().formatHex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Hashing failed", e);
        }
    }
}
