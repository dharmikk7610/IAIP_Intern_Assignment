package com.ems.mapper;

import com.ems.dto.EmployeePersonalDetailsResponse;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.TransferHistoryResponse;
import com.ems.entity.Employee;
import com.ems.entity.EmployeePersonalDetails;
import com.ems.entity.EmployeeTransferHistory;
import org.springframework.stereotype.Component;

/** Entity -> DTO conversion. Call inside a transaction: department/role are lazy. */
@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getMobile(),
                employee.getGender(),
                employee.getAddress(),
                employee.getDepartment().getId(),
                employee.getDepartment().getName(),
                employee.getRole().getId(),
                employee.getRole().getName(),
                employee.getJoiningDate(),
                employee.getAnnualSalary(),
                employee.getStatus(),
                employee.getVersion(),
                employee.getCreatedAt(),
                employee.getUpdatedAt());
    }

    public TransferHistoryResponse toTransferResponse(EmployeeTransferHistory history) {
        return new TransferHistoryResponse(
                history.getId(),
                history.getEmployee().getId(),
                history.getEmployee().getFullName(),
                history.getOldDepartment().getId(),
                history.getOldDepartment().getName(),
                history.getNewDepartment().getId(),
                history.getNewDepartment().getName(),
                history.getOldEmployeeCode(),
                history.getNewEmployeeCode(),
                history.getTransferDate(),
                history.getReason(),
                history.getTransferredBy() == null ? null : history.getTransferredBy().getUsername(),
                history.getCreatedAt());
    }

    public EmployeePersonalDetailsResponse toPersonalDetailsResponse(Employee employee,
                                                                     EmployeePersonalDetails details,
                                                                     boolean revealSensitive) {
        String aadhaar = revealSensitive
                ? SensitiveDataMasker.formatAadhaar(details.getAadhaarNumber())
                : SensitiveDataMasker.maskAadhaar(details.getAadhaarNumber());
        String pan = revealSensitive
                ? details.getPanNumber()
                : SensitiveDataMasker.maskPan(details.getPanNumber());
        return new EmployeePersonalDetailsResponse(
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getFullName(),
                aadhaar,
                pan,
                details.getDateOfBirth(),
                !revealSensitive);
    }
}
