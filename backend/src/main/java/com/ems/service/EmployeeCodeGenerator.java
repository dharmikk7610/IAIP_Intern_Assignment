package com.ems.service;

import com.ems.entity.Department;
import com.ems.entity.EmployeeCodeSequence;
import com.ems.repository.EmployeeCodeSequenceRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Allocates department-wise employee codes (I0001, H0001, ...).
 * <p>
 * MANDATORY propagation: it must join the caller's transaction, so the sequence row
 * stays locked until the employee row is committed. If anything later fails, the
 * whole transaction rolls back and the sequence increment is undone too.
 */
@Component
public class EmployeeCodeGenerator {

    private final EmployeeCodeSequenceRepository sequenceRepository;

    public EmployeeCodeGenerator(EmployeeCodeSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public String nextCodeFor(Department department) {
        EmployeeCodeSequence sequence = sequenceRepository.findByDepartmentIdForUpdate(department.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "No employee code sequence configured for department " + department.getName()));
        return sequence.allocateNextCode();
    }
}
