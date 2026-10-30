package com.ems.service;

import com.ems.dto.TransferHistoryResponse;
import com.ems.dto.TransferRequest;
import com.ems.dto.TransferResponse;
import com.ems.entity.Department;
import com.ems.entity.Employee;
import com.ems.entity.EmployeeStatus;
import com.ems.entity.EmployeeTransferHistory;
import com.ems.exception.BusinessValidationException;
import com.ems.exception.ConflictException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.mapper.EmployeeMapper;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.EmployeeTransferHistoryRepository;
import com.ems.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class EmployeeTransferService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeTransferHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final EmployeeCodeGenerator codeGenerator;
    private final EmployeeMapper mapper;

    public EmployeeTransferService(EmployeeRepository employeeRepository,
                                   DepartmentRepository departmentRepository,
                                   EmployeeTransferHistoryRepository historyRepository,
                                   UserRepository userRepository,
                                   EmployeeCodeGenerator codeGenerator,
                                   EmployeeMapper mapper) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.codeGenerator = codeGenerator;
        this.mapper = mapper;
    }

    /**
     * Moves an employee to another department in ONE transaction:
     * <ol>
     *   <li>lock the employee row (concurrent transfers of the same person queue up)</li>
     *   <li>validate the transfer</li>
     *   <li>lock the destination department's sequence and take its next code</li>
     *   <li>update department + code (the internal id never changes)</li>
     *   <li>insert the history row</li>
     * </ol>
     * If any step fails, everything is rolled back, including the sequence increment.
     *
     * @param transferredByUserId the logged-in user, or null for system actions (seed data)
     */
    @Transactional
    public TransferResponse transfer(Long employeeId, TransferRequest request, Long transferredByUserId) {
        Employee employee = employeeRepository.findByIdForUpdate(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + employeeId));

        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new ConflictException("Inactive employees cannot be transferred");
        }
        Department oldDepartment = employee.getDepartment();
        if (oldDepartment.getId().equals(request.newDepartmentId())) {
            throw new BusinessValidationException("newDepartmentId",
                    "Employee is already in the " + oldDepartment.getName() + " department");
        }
        Department newDepartment = departmentRepository.findById(request.newDepartmentId())
                .orElseThrow(() -> new BusinessValidationException("newDepartmentId", "Department does not exist"));
        validateTransferDate(employee, request.transferDate());

        String oldCode = employee.getEmployeeCode();
        String newCode = codeGenerator.nextCodeFor(newDepartment);
        employee.setDepartment(newDepartment);
        employee.setEmployeeCode(newCode);

        EmployeeTransferHistory history = new EmployeeTransferHistory();
        history.setEmployee(employee);
        history.setOldDepartment(oldDepartment);
        history.setNewDepartment(newDepartment);
        history.setOldEmployeeCode(oldCode);
        history.setNewEmployeeCode(newCode);
        history.setTransferDate(request.transferDate());
        history.setReason(request.reason().trim());
        if (transferredByUserId != null) {
            history.setTransferredBy(userRepository.getReferenceById(transferredByUserId));
        }
        historyRepository.save(history);

        employeeRepository.flush();
        return new TransferResponse(mapper.toResponse(employee), mapper.toTransferResponse(history));
    }

    @Transactional(readOnly = true)
    public List<TransferHistoryResponse> history(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee not found with id " + employeeId);
        }
        return historyRepository.findByEmployeeIdOrderByTransferDateDescIdDesc(employeeId).stream()
                .map(mapper::toTransferResponse)
                .toList();
    }

    /** Not in the future, not before joining, not before the previous transfer (keeps history in order). */
    private void validateTransferDate(Employee employee, LocalDate transferDate) {
        if (transferDate.isAfter(LocalDate.now())) {
            throw new BusinessValidationException("transferDate", "Transfer date cannot be in the future");
        }
        if (transferDate.isBefore(employee.getJoiningDate())) {
            throw new BusinessValidationException("transferDate",
                    "Transfer date cannot be before the joining date (" + employee.getJoiningDate() + ")");
        }
        historyRepository.findFirstByEmployeeIdOrderByTransferDateDescIdDesc(employee.getId())
                .filter(last -> transferDate.isBefore(last.getTransferDate()))
                .ifPresent(last -> {
                    throw new BusinessValidationException("transferDate",
                            "Transfer date cannot be before the previous transfer (" + last.getTransferDate() + ")");
                });
    }
}
