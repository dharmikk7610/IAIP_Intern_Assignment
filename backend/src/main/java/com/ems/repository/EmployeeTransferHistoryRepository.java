package com.ems.repository;

import com.ems.entity.EmployeeTransferHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeTransferHistoryRepository extends JpaRepository<EmployeeTransferHistory, Long> {

    @EntityGraph(attributePaths = {"employee", "oldDepartment", "newDepartment", "transferredBy"})
    List<EmployeeTransferHistory> findByEmployeeIdOrderByTransferDateDescIdDesc(Long employeeId);

    Optional<EmployeeTransferHistory> findFirstByEmployeeIdOrderByTransferDateDescIdDesc(Long employeeId);
}
