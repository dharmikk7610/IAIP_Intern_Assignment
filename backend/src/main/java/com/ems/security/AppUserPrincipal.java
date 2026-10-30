package com.ems.security;

import com.ems.entity.UserRole;

/**
 * The authenticated user, rebuilt from JWT claims on every request (no server-side session).
 * employeeId is set for users linked to an employee record (always for EMPLOYEE role).
 */
public record AppUserPrincipal(Long userId, String username, UserRole role, Long employeeId) {
}
