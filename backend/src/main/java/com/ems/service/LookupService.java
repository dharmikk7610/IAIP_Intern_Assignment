package com.ems.service;

import com.ems.dto.DepartmentResponse;
import com.ems.dto.RoleResponse;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.RoleRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read-only reference data for dropdowns. */
@Service
public class LookupService {

    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;

    public LookupService(DepartmentRepository departmentRepository, RoleRepository roleRepository) {
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
    }

    @Transactional(readOnly = true)
    public List<DepartmentResponse> departments() {
        return departmentRepository.findAll(Sort.by("id")).stream()
                .map(d -> new DepartmentResponse(d.getId(), d.getName(), d.getCodePrefix()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> roles() {
        return roleRepository.findAll(Sort.by("name")).stream()
                .map(r -> new RoleResponse(r.getId(), r.getName()))
                .toList();
    }
}
