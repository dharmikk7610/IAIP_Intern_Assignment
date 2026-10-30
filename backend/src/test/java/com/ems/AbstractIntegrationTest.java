package com.ems;

import com.ems.entity.Employee;
import com.ems.entity.User;
import com.ems.entity.UserRole;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.UserRepository;
import com.ems.security.AppUserPrincipal;
import com.ems.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Runs against a real MySQL database (EMPLOYEEMNG_TEST, created automatically).
 * Before each test: reference data is loaded, all employee/user data is wiped and
 * fresh ADMIN and MANAGER users are created. Sharing one base class lets Spring
 * reuse a single application context for all test classes.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Sql(scripts = "file:../database/02_seed_data.sql", statements = {
        "DELETE FROM employee_transfer_history",
        "DELETE FROM users",
        "DELETE FROM employee_personal_details",
        "DELETE FROM employees",
        "UPDATE employee_code_sequences SET next_value = 1"
})
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_PASSWORD = "Admin@123";

    @Autowired
    protected MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private JwtService jwtService;

    protected String adminToken;
    protected String managerToken;

    @BeforeEach
    void createDefaultUsers() {
        adminToken = tokenFor(createUser("admin", ADMIN_PASSWORD, UserRole.ADMIN, null));
        managerToken = tokenFor(createUser("manager", "Manager@123", UserRole.MANAGER, null));
    }

    /** Creates an EMPLOYEE login linked to the given employee and returns its bearer header value. */
    protected String employeeToken(long employeeId) {
        Employee employee = employeeRepository.findById(employeeId).orElseThrow();
        return tokenFor(createUser("emp" + employeeId, "Employee@123", UserRole.EMPLOYEE, employee));
    }

    protected ResultActions asAdmin(MockHttpServletRequestBuilder request) throws Exception {
        return as(adminToken, request);
    }

    protected ResultActions as(String bearerToken, MockHttpServletRequestBuilder request) throws Exception {
        return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, bearerToken));
    }

    private User createUser(String username, String password, UserRole role, Employee employee) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUserRole(role);
        user.setEmployee(employee);
        return userRepository.save(user);
    }

    private String tokenFor(User user) {
        AppUserPrincipal principal = new AppUserPrincipal(user.getId(), user.getUsername(), user.getUserRole(),
                user.getEmployee() == null ? null : user.getEmployee().getId());
        return "Bearer " + jwtService.issue(principal).token();
    }
}
