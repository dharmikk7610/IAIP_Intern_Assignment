package com.ems.repository;

import com.ems.entity.EmployeeCodeSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EmployeeCodeSequenceRepository extends JpaRepository<EmployeeCodeSequence, Long> {

    /**
     * SELECT ... FOR UPDATE: the row stays locked until the surrounding transaction ends,
     * so concurrent create/transfer requests for the same department queue up here.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from EmployeeCodeSequence s where s.departmentId = :departmentId")
    Optional<EmployeeCodeSequence> findByDepartmentIdForUpdate(@Param("departmentId") Long departmentId);
}
