package com.ems.repository;

import com.ems.entity.EmployeePersonalDetails;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeePersonalDetailsRepository extends JpaRepository<EmployeePersonalDetails, Long> {

    boolean existsByAadhaarHash(String aadhaarHash);

    boolean existsByAadhaarHashAndEmployeeIdNot(String aadhaarHash, Long employeeId);

    boolean existsByPanHash(String panHash);

    boolean existsByPanHashAndEmployeeIdNot(String panHash, Long employeeId);
}
