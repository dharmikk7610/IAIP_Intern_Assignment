package com.ems.security.crypto;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmEncryptorTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private final AesGcmEncryptor encryptor = new AesGcmEncryptor(KEY);

    @Test
    void encryptThenDecryptReturnsOriginal() {
        String cipherText = encryptor.encrypt("123456789012");

        assertThat(cipherText).doesNotContain("123456789012");
        assertThat(encryptor.decrypt(cipherText)).isEqualTo("123456789012");
    }

    @Test
    void sameValueEncryptsDifferentlyEachTime() {
        assertThat(encryptor.encrypt("ABCDE1234F")).isNotEqualTo(encryptor.encrypt("ABCDE1234F"));
    }

    @Test
    void tamperedCipherTextIsRejected() {
        byte[] bytes = Base64.getDecoder().decode(encryptor.encrypt("ABCDE1234F"));
        bytes[bytes.length - 1] ^= 1;

        assertThatThrownBy(() -> encryptor.decrypt(Base64.getEncoder().encodeToString(bytes)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsWrongKeyLength() {
        assertThatThrownBy(() -> new AesGcmEncryptor(Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class);
    }
}
