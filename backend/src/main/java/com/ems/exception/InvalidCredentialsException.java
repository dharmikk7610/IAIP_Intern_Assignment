package com.ems.exception;

/**
 * -> 401. Same message for unknown user, wrong password, disabled account and a login whose
 * employee is INACTIVE (no user enumeration).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid username or password");
    }
}
