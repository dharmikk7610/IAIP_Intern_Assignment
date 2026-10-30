package com.ems.security.crypto;

import jakarta.persistence.AttributeConverter;
import org.springframework.stereotype.Component;

/**
 * JPA converter applied with {@code @Convert}: entities hold plain values,
 * the database only ever sees ciphertext. Spring Boot lets Hibernate obtain
 * this converter as a Spring bean, so the encryptor can be injected.
 */
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final AesGcmEncryptor encryptor;

    public EncryptedStringConverter(AesGcmEncryptor encryptor) {
        this.encryptor = encryptor;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : encryptor.decrypt(dbData);
    }
}
