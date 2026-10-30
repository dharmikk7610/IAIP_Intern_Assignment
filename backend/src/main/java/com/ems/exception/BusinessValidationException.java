package com.ems.exception;

import java.util.Map;

/** -> 400 Bad Request for rules that annotations cannot express (cross-field or database checks). */
public class BusinessValidationException extends RuntimeException {

    private final Map<String, String> fieldErrors;

    public BusinessValidationException(String field, String message) {
        this(Map.of(field, message));
    }

    public BusinessValidationException(Map<String, String> fieldErrors) {
        super("Validation failed");
        this.fieldErrors = Map.copyOf(fieldErrors);
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
