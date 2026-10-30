package com.ems.dto;

import com.ems.entity.UserRole;

public record CurrentUserResponse(Long userId, String username, UserRole role, Long employeeId) {
}
