package com.ems.service;

import com.ems.dto.CurrentUserResponse;
import com.ems.dto.LoginRequest;
import com.ems.dto.LoginResponse;
import com.ems.entity.EmployeeStatus;
import com.ems.entity.User;
import com.ems.exception.InvalidCredentialsException;
import com.ems.repository.UserRepository;
import com.ems.security.AppUserPrincipal;
import com.ems.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Compared against when the user doesn't exist, so the response time doesn't reveal valid usernames. */
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyHash = passwordEncoder.encode("not-a-real-password");
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByUsername(request.username().trim());
        String hash = found.map(User::getPasswordHash).orElse(dummyHash);
        boolean passwordMatches = passwordEncoder.matches(request.password(), hash);
        // A login linked to a deactivated employee is treated like a disabled account
        User user = found.filter(u -> passwordMatches && u.isEnabled()
                        && (u.getEmployee() == null || u.getEmployee().getStatus() != EmployeeStatus.INACTIVE))
                .orElseThrow(InvalidCredentialsException::new);

        AppUserPrincipal principal = new AppUserPrincipal(user.getId(), user.getUsername(), user.getUserRole(),
                user.getEmployee() == null ? null : user.getEmployee().getId());
        JwtService.IssuedToken token = jwtService.issue(principal);
        return new LoginResponse(token.token(), "Bearer", token.expiresAt(), toResponse(principal));
    }

    public CurrentUserResponse toResponse(AppUserPrincipal principal) {
        return new CurrentUserResponse(principal.userId(), principal.username(), principal.role(),
                principal.employeeId());
    }
}
