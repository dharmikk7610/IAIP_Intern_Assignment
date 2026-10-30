package com.ems.dto;

import java.time.LocalDate;

/**
 * Sensitive personal details. Aadhaar / PAN are masked (XXXX-XXXX-9012, XXXXXX234F)
 * unless the caller is allowed to see full values; {@code masked} tells the UI which.
 */
public record EmployeePersonalDetailsResponse(
        Long employeeId,
        String employeeCode,
        String fullName,
        String aadhaarNumber,
        String panNumber,
        LocalDate dateOfBirth,
        boolean masked
) {
}
