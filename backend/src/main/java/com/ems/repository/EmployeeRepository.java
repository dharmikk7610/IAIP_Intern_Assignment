package com.ems.repository;

import com.ems.entity.Employee;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/** JpaSpecificationExecutor lets the service combine search + filters dynamically (Milestone 4). */
public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    /** Loads department and role in the same query, avoiding one extra query per row (N+1). */
    @Override
    @EntityGraph(attributePaths = {"department", "role"})
    Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"department", "role"})
    Optional<Employee> findWithDepartmentAndRoleById(Long id);

    /**
     * SELECT ... FOR UPDATE on the employee row, so two transfers of the same employee
     * run one after the other and the second sees the first one's result.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Employee e where e.id = :id")
    Optional<Employee> findByIdForUpdate(@Param("id") Long id);

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);
}
