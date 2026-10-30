package com.ems.dto;

import com.ems.entity.EmployeeStatus;
import com.ems.entity.Gender;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Query parameters of GET /api/employees. Every field is optional; all given filters are combined with AND.
 * Salary range: minSalary is inclusive, maxSalary is exclusive ("3-5 LPA" = 300000 <= salary < 500000).
 * Joining date range: both ends inclusive.
 */
public record EmployeeSearchCriteria(
        String search,
        Long departmentId,
        Long roleId,
        EmployeeStatus status,
        Gender gender,
        LocalDate joinedFrom,
        LocalDate joinedTo,
        BigDecimal minSalary,
        BigDecimal maxSalary,
        String sortBy,
        String sortDir,
        int pageNumber,
        int pageSize
) {
}
