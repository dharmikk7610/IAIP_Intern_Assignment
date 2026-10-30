package com.ems.security.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataHasherTest {

    private final SensitiveDataHasher hasher =
            new SensitiveDataHasher(Base64.getEncoder().encodeToString(new byte[32]));

    @Test
    void sameInputGivesSameHash() {
        assertThat(hasher.hash("123456789012")).isEqualTo(hasher.hash("123456789012")).hasSize(64);
    }

    @Test
    void differentInputGivesDifferentHash() {
        assertThat(hasher.hash("123456789012")).isNotEqualTo(hasher.hash("123456789013"));
    }
}
