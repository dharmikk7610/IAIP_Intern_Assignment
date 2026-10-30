package com.ems.controller;

import com.ems.dto.TransferHistoryResponse;
import com.ems.dto.TransferRequest;
import com.ems.dto.TransferResponse;
import com.ems.security.AppUserPrincipal;
import com.ems.service.EmployeeTransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees/{id}")
@Tag(name = "Department transfers")
public class EmployeeTransferController {

    private final EmployeeTransferService transferService;

    public EmployeeTransferController(EmployeeTransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Transfer an employee to another department (generates a new employee code)")
    public TransferResponse transfer(@PathVariable Long id, @Valid @RequestBody TransferRequest request,
                                     @AuthenticationPrincipal AppUserPrincipal principal) {
        return transferService.transfer(id, request, principal.userId());
    }

    @GetMapping("/transfers")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER') or @employeeAccess.isSelf(authentication, #id)")
    @Operation(summary = "Transfer history of an employee, newest first")
    public List<TransferHistoryResponse> history(@PathVariable Long id) {
        return transferService.history(id);
    }
}
