package com.ems.dto;

/** Result of a transfer: the updated employee (new code / department) and the history entry written. */
public record TransferResponse(EmployeeResponse employee, TransferHistoryResponse transfer) {
}
