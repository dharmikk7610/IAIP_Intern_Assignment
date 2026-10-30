package com.ems.controller;

import com.ems.dto.DepartmentResponse;
import com.ems.dto.RoleResponse;
import com.ems.service.LookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Lookups", description = "Departments and roles for dropdowns and filters")
public class LookupController {

    private final LookupService lookupService;

    public LookupController(LookupService lookupService) {
        this.lookupService = lookupService;
    }

    @GetMapping("/departments")
    @Operation(summary = "List departments")
    public List<DepartmentResponse> departments() {
        return lookupService.departments();
    }

    @GetMapping("/roles")
    @Operation(summary = "List roles / designations")
    public List<RoleResponse> roles() {
        return lookupService.roles();
    }
}
