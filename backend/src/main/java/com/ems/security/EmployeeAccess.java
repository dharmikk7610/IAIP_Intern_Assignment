package com.ems.security;

import com.ems.entity.UserRole;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Record-level access rules, used from @PreAuthorize expressions as {@code @employeeAccess}.
 * Role rules alone can't express "an EMPLOYEE may read only their own record".
 */
@Component("employeeAccess")
public class EmployeeAccess {

    public boolean isSelf(Authentication authentication, Long employeeId) {
        return employeeId != null
                && authentication != null
                && authentication.getPrincipal() instanceof AppUserPrincipal user
                && employeeId.equals(user.employeeId());
    }

    /** Only ADMIN may deactivate or reactivate an employee (also through PUT). */
    public boolean isAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getPrincipal() instanceof AppUserPrincipal user
                && user.role() == UserRole.ADMIN;
    }

    /** Full Aadhaar / PAN: ADMIN, or the employee viewing their own record. Everyone else sees masked values. */
    public boolean canSeeFullSensitiveData(Authentication authentication, Long employeeId) {
        return isSelf(authentication, employeeId)
                || (authentication != null
                && authentication.getPrincipal() instanceof AppUserPrincipal user
                && user.role() == UserRole.ADMIN);
    }
}
