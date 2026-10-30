package com.ems.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TransferHistoryResponse(
        Long id,
        Long employeeId,
        String employeeName,
        Long oldDepartmentId,
        String oldDepartmentName,
        Long newDepartmentId,
        String newDepartmentName,
        String oldEmployeeCode,
        String newEmployeeCode,
        LocalDate transferDate,
        String reason,
        /* Username of whoever performed the transfer, null for system / seed data. */
        String transferredBy,
        LocalDateTime createdAt
) {
}
