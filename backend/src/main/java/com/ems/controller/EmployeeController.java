package com.ems.controller;

import com.ems.dto.EmployeePersonalDetailsResponse;
import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.EmployeeSearchCriteria;
import com.ems.dto.PageResponse;
import com.ems.entity.EmployeeStatus;
import com.ems.entity.Gender;
import com.ems.security.EmployeeAccess;
import com.ems.service.EmployeeService;
import com.ems.validation.OnCreate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.groups.Default;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;

/**
 * HTTP layer only: validation of the body, status codes, access rules, delegation to EmployeeService.
 * Access: ADMIN and MANAGER manage employees, only ADMIN deactivates,
 * an EMPLOYEE may read only their own record.
 */
@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Create, view, update and deactivate employees")
public class EmployeeController {

    private static final String ADMIN_MANAGER_OR_SELF =
            "hasAnyRole('ADMIN', 'MANAGER') or @employeeAccess.isSelf(authentication, #id)";

    private final EmployeeService employeeService;
    private final EmployeeAccess employeeAccess;

    public EmployeeController(EmployeeService employeeService, EmployeeAccess employeeAccess) {
        this.employeeService = employeeService;
        this.employeeAccess = employeeAccess;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Create an employee (employee code is generated from the department)")
    public ResponseEntity<EmployeeResponse> create(
            @Validated({Default.class, OnCreate.class}) @RequestBody EmployeeRequest request) {
        EmployeeResponse created = employeeService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Search, filter, sort and page employees",
            description = "All parameters are optional and combined with AND. Global search covers code, name, "
                    + "email, mobile, address, department, role and status (never Aadhaar / PAN).")
    public PageResponse<EmployeeResponse> search(
            @Parameter(description = "Case-insensitive text matched against permitted fields")
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long roleId,
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) Gender gender,
            @Parameter(description = "Joined on or after (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinedFrom,
            @Parameter(description = "Joined on or before (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinedTo,
            @Parameter(description = "Annual salary >= minSalary (inclusive)")
            @RequestParam(required = false) BigDecimal minSalary,
            @Parameter(description = "Annual salary < maxSalary (exclusive), e.g. 3-5 LPA = 300000 to 500000")
            @RequestParam(required = false) BigDecimal maxSalary,
            @Parameter(description = "id, employeeCode, fullName, email, mobile, departmentName, roleName, "
                    + "joiningDate, annualSalary, status")
            @RequestParam(required = false) String sortBy,
            @Parameter(description = "asc or desc")
            @RequestParam(required = false) String sortDir,
            @Parameter(description = "1-based page number")
            @RequestParam(defaultValue = "1") int pageNumber,
            @Parameter(description = "Records per page (UI offers 5, 10, 20)")
            @RequestParam(defaultValue = "10") int pageSize) {
        return employeeService.search(new EmployeeSearchCriteria(search, departmentId, roleId, status, gender,
                joinedFrom, joinedTo, minSalary, maxSalary, sortBy, sortDir, pageNumber, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize(ADMIN_MANAGER_OR_SELF)
    @Operation(summary = "Get employment details of one employee")
    public EmployeeResponse getById(@PathVariable Long id) {
        return employeeService.getById(id);
    }

    @GetMapping("/{id}/personal-details")
    @PreAuthorize(ADMIN_MANAGER_OR_SELF)
    @Operation(summary = "Get personal details (Aadhaar / PAN full for ADMIN and the employee, masked otherwise)")
    public EmployeePersonalDetailsResponse getPersonalDetails(@PathVariable Long id, Authentication authentication) {
        return employeeService.getPersonalDetails(id, employeeAccess.canSeeFullSensitiveData(authentication, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    @Operation(summary = "Update an employee (department changes go through transfer)")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request,
                                   Authentication authentication) {
        return employeeService.update(id, request, employeeAccess.isAdmin(authentication));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Deactivate an employee (soft delete)")
    public EmployeeResponse deactivate(@PathVariable Long id) {
        return employeeService.deactivate(id);
    }
}
