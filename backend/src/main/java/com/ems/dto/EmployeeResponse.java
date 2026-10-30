package com.ems.dto;

import com.ems.entity.EmployeeStatus;
import com.ems.entity.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Employment and general details. Never contains Aadhaar / PAN. */
public record EmployeeResponse(
        Long id,
        String employeeCode,
        String fullName,
        String email,
        String mobile,
        Gender gender,
        String address,
        Long departmentId,
        String departmentName,
        Long roleId,
        String roleName,
        LocalDate joiningDate,
        BigDecimal annualSalary,
        EmployeeStatus status,
        Long version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
