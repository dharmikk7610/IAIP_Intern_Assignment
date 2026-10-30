package com.ems.exception;

/** -> 409 Conflict: the request is valid but clashes with existing data (duplicates, stale edits). */
public class ConflictException extends RuntimeException {

    /** Request field the conflict relates to, or null. */
    private final String field;

    public ConflictException(String message) {
        this(null, message);
    }

    public ConflictException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
