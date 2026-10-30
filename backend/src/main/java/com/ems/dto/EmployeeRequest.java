package com.ems.dto;

import com.ems.entity.EmployeeStatus;
import com.ems.entity.Gender;
import com.ems.validation.OnCreate;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Body for create (POST) and update (PUT).
 * Field-level rules live here; rules that need the database or several fields
 * (duplicates, minimum age, department change) are checked in EmployeeService.
 */
public record EmployeeRequest(

        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        @Pattern(regexp = "^[A-Za-z ]+$", message = "Full name may contain only alphabets and spaces")
        String fullName,

        @NotBlank(message = "Email is required")
        @Size(max = 150, message = "Email must be at most 150 characters")
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$",
                message = "Email must be a valid email address")
        String email,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^\\d{10}$", message = "Mobile number must be exactly 10 digits")
        String mobile,

        /* Accepts 123456789012 or 1234-5678-9012; stored as 12 digits. Optional on update. */
        @NotBlank(groups = OnCreate.class, message = "Aadhaar number is required")
        @Pattern(regexp = "^\\d{4}-?\\d{4}-?\\d{4}$",
                message = "Aadhaar number must be exactly 12 digits (XXXX-XXXX-XXXX)")
        String aadhaarNumber,

        /* Case-insensitive input, stored upper-case. Optional on update. */
        @NotBlank(groups = OnCreate.class, message = "PAN number is required")
        @Pattern(regexp = "^[A-Za-z]{5}\\d{4}[A-Za-z]$", message = "PAN must be in the format ABCDE1234F")
        String panNumber,

        @NotNull(message = "Date of birth is required")
        @PastOrPresent(message = "Date of birth cannot be a future date")
        LocalDate dateOfBirth,

        @NotNull(message = "Gender is required")
        Gender gender,

        @NotBlank(message = "Address is required")
        @Size(max = 500, message = "Address must be at most 500 characters")
        String address,

        @NotNull(message = "Department is required")
        Long departmentId,

        @NotNull(message = "Role / designation is required")
        Long roleId,

        @NotNull(message = "Joining date is required")
        LocalDate joiningDate,

        @NotNull(message = "Annual salary is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "Annual salary must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Annual salary may have at most 10 digits and 2 decimals")
        BigDecimal annualSalary,

        /* Defaults to ACTIVE on create. */
        EmployeeStatus status,

        /* Update only: the version the client loaded, to detect concurrent edits. */
        Long version
) {
}
