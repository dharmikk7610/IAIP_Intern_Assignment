package com.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TransferRequest(

        @NotNull(message = "New department is required")
        Long newDepartmentId,

        @NotNull(message = "Transfer date is required")
        @PastOrPresent(message = "Transfer date cannot be in the future")
        LocalDate transferDate,

        @NotBlank(message = "Transfer reason is required")
        @Size(max = 500, message = "Transfer reason must be at most 500 characters")
        String reason
) {
}
