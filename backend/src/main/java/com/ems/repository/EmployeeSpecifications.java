package com.ems.repository;

import com.ems.dto.EmployeeSearchCriteria;
import com.ems.entity.Department;
import com.ems.entity.Employee;
import com.ems.entity.EmployeeStatus;
import com.ems.entity.Role;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Builds the WHERE clause for employee search. Each supplied filter adds one predicate and
 * all predicates are AND-ed, so search, filters and pagination always work together.
 * Values are bound as JDBC parameters, never concatenated into SQL.
 */
public final class EmployeeSpecifications {

    private static final char LIKE_ESCAPE = '!';

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> matching(EmployeeSearchCriteria c) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (c.search() != null && !c.search().isBlank()) {
                predicates.add(globalSearch(root, cb, c.search().trim()));
            }
            if (c.departmentId() != null) {
                predicates.add(cb.equal(root.get("department").get("id"), c.departmentId()));
            }
            if (c.roleId() != null) {
                predicates.add(cb.equal(root.get("role").get("id"), c.roleId()));
            }
            if (c.status() != null) {
                predicates.add(cb.equal(root.get("status"), c.status()));
            }
            if (c.gender() != null) {
                predicates.add(cb.equal(root.get("gender"), c.gender()));
            }
            if (c.joinedFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("joiningDate"), c.joinedFrom()));
            }
            if (c.joinedTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("joiningDate"), c.joinedTo()));
            }
            if (c.minSalary() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("annualSalary"), c.minSalary()));
            }
            if (c.maxSalary() != null) {
                predicates.add(cb.lessThan(root.get("annualSalary"), c.maxSalary()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Case-insensitive "contains" across the permitted fields. Aadhaar and PAN live in another
     * table that is never joined here, so they can't be matched.
     */
    private static Predicate globalSearch(Root<Employee> root, CriteriaBuilder cb, String term) {
        String pattern = "%" + escapeLike(term.toLowerCase(Locale.ROOT)) + "%";
        Join<Employee, Department> department = root.join("department", JoinType.INNER);
        Join<Employee, Role> role = root.join("role", JoinType.INNER);

        List<Predicate> anyOf = new ArrayList<>(List.of(
                contains(cb, root.get("employeeCode"), pattern),
                contains(cb, root.get("fullName"), pattern),
                contains(cb, root.get("email"), pattern),
                contains(cb, root.get("mobile"), pattern),
                contains(cb, root.get("address"), pattern),
                contains(cb, department.get("name"), pattern),
                contains(cb, role.get("name"), pattern)));

        List<EmployeeStatus> statuses = statusesMatching(term);
        if (!statuses.isEmpty()) {
            anyOf.add(root.get("status").in(statuses));
        }
        return cb.or(anyOf.toArray(Predicate[]::new));
    }

    private static Predicate contains(CriteriaBuilder cb, Expression<String> field, String pattern) {
        return cb.like(cb.lower(field), pattern, LIKE_ESCAPE);
    }

    /** Makes %, _ and the escape character match literally instead of acting as wildcards. */
    static String escapeLike(String value) {
        return value.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    /**
     * Statuses whose name or any of its words starts with the term:
     * "active" -> ACTIVE (not INACTIVE), "leave" / "on leave" -> ON_LEAVE.
     */
    static List<EmployeeStatus> statusesMatching(String term) {
        String normalized = term.toUpperCase(Locale.ROOT).replaceAll("[\\s-]+", "_");
        return Arrays.stream(EmployeeStatus.values())
                .filter(s -> s.name().startsWith(normalized)
                        || Arrays.stream(s.name().split("_")).anyMatch(word -> word.startsWith(normalized)))
                .toList();
    }
}
