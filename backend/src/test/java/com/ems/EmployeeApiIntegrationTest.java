package com.ems;

import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.entity.EmployeeStatus;
import com.ems.entity.Gender;
import com.ems.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployeeApiIntegrationTest extends AbstractIntegrationTest {

    private static final long IT = 1, HR = 2, EMPLOYEE_ROLE = 3;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EmployeeService employeeService;

    @Test
    void createGeneratesDepartmentCodeAndStoresAadhaarEncrypted() throws Exception {
        createEmployee(request("Rahul Sharma", "rahul@example.com", "1234-5678-9012", "abcde1234f", IT))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.employeeCode").value("I0001"))
                .andExpect(jsonPath("$.departmentName").value("IT"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.aadhaarNumber").doesNotExist());

        createEmployee(request("Priya Nair", "priya@example.com", "123456789013", "ABCDE1234G", IT))
                .andExpect(jsonPath("$.employeeCode").value("I0002"));
        createEmployee(request("Kavita Joshi", "kavita@example.com", "123456789014", "ABCDE1234H", HR))
                .andExpect(jsonPath("$.employeeCode").value("H0001"));

        String stored = jdbcTemplate.queryForObject(
                "SELECT aadhaar_encrypted FROM employee_personal_details d JOIN employees e ON e.id = d.employee_id "
                        + "WHERE e.employee_code = 'I0001'", String.class);
        assertThat(stored).doesNotContain("123456789012");
    }

    @Test
    void personalDetailsAreMaskedForManager() throws Exception {
        long id = createAndGetId(request("Rahul Sharma", "rahul@example.com", "123456789012", "ABCDE1234F", IT));

        as(managerToken, get("/api/employees/{id}/personal-details", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aadhaarNumber").value("XXXX-XXXX-9012"))
                .andExpect(jsonPath("$.panNumber").value("XXXXXX234F"))
                .andExpect(jsonPath("$.masked").value(true));
    }

    @Test
    void duplicateEmailIsRejectedIgnoringCase() throws Exception {
        createEmployee(request("Rahul Sharma", "rahul@example.com", "123456789012", "ABCDE1234F", IT));

        createEmployee(request("Rahul Verma", "RAHUL@Example.com", "123456789013", "ABCDE1234G", IT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void duplicateAadhaarIsRejectedEvenWhenFormattedDifferently() throws Exception {
        createEmployee(request("Rahul Sharma", "rahul@example.com", "123456789012", "ABCDE1234F", IT));

        createEmployee(request("Rahul Verma", "verma@example.com", "1234-5678-9012", "ABCDE1234G", IT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fieldErrors.aadhaarNumber").exists());
    }

    @Test
    void invalidFieldsReturn400WithAMessagePerField() throws Exception {
        EmployeeRequest invalid = new EmployeeRequest("Rahul 123", "not-an-email", "98765", "1234", "ABC123",
                LocalDate.now().plusDays(1), null, "", IT, EMPLOYEE_ROLE, LocalDate.now(),
                BigDecimal.ZERO, null, null);

        createEmployee(invalid)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.mobile").exists())
                .andExpect(jsonPath("$.fieldErrors.aadhaarNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.panNumber").exists())
                .andExpect(jsonPath("$.fieldErrors.dateOfBirth").exists())
                .andExpect(jsonPath("$.fieldErrors.gender").exists())
                .andExpect(jsonPath("$.fieldErrors.address").exists())
                .andExpect(jsonPath("$.fieldErrors.annualSalary").exists());

        assertThat(jdbcTemplate.queryForObject("SELECT next_value FROM employee_code_sequences WHERE department_id = 1",
                Integer.class)).as("a rejected request must not use up a code").isEqualTo(1);
    }

    @Test
    void employeeMustBeAtLeast18() throws Exception {
        EmployeeRequest tooYoung = withDates(request("Rahul Sharma", "rahul@example.com", "123456789012",
                "ABCDE1234F", IT), LocalDate.now().minusYears(17), LocalDate.now());

        createEmployee(tooYoung)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dateOfBirth").value("Employee must be at least 18 years old"));
    }

    @Test
    void updateChangesFieldsButNotDepartment() throws Exception {
        long id = createAndGetId(request("Rahul Sharma", "rahul@example.com", "123456789012", "ABCDE1234F", IT));

        // Aadhaar / PAN omitted on update -> kept as they are
        EmployeeRequest update = new EmployeeRequest("Rahul  Kumar  Sharma", "rahul.k@example.com", "9999999999",
                null, null, LocalDate.of(1995, 4, 12), Gender.MALE, "New address", IT, EMPLOYEE_ROLE,
                LocalDate.of(2021, 6, 1), new BigDecimal("700000"), EmployeeStatus.ON_LEAVE, null);
        asAdmin(put("/api/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Rahul Kumar Sharma"))
                .andExpect(jsonPath("$.annualSalary").value(700000))
                .andExpect(jsonPath("$.status").value("ON_LEAVE"))
                .andExpect(jsonPath("$.employeeCode").value("I0001"));
        as(managerToken, get("/api/employees/{id}/personal-details", id))
                .andExpect(jsonPath("$.aadhaarNumber").value("XXXX-XXXX-9012"));

        EmployeeRequest moveDepartment = new EmployeeRequest("Rahul Sharma", "rahul.k@example.com", "9999999999",
                null, null, LocalDate.of(1995, 4, 12), Gender.MALE, "New address", HR, EMPLOYEE_ROLE,
                LocalDate.of(2021, 6, 1), new BigDecimal("700000"), null, null);
        asAdmin(put("/api/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(moveDepartment)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.departmentId").exists());
    }

    @Test
    void staleVersionOnUpdateReturns409() throws Exception {
        EmployeeRequest create = request("Rahul Sharma", "rahul@example.com", "123456789012", "ABCDE1234F", IT);
        long id = createAndGetId(create);

        EmployeeRequest stale = new EmployeeRequest(create.fullName(), create.email(), create.mobile(), null, null,
                create.dateOfBirth(), create.gender(), create.address(), IT, EMPLOYEE_ROLE,
                create.joiningDate(), create.annualSalary(), null, 99L);
        asAdmin(put("/api/employees/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stale)))
                .andExpect(status().isConflict());
    }

    @Test
    void deactivateIsASoftDelete() throws Exception {
        long id = createAndGetId(request("Rahul Sharma", "rahul@example.com", "123456789012", "ABCDE1234F", IT));

        asAdmin(patch("/api/employees/{id}/deactivate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
        asAdmin(get("/api/employees/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void unknownEmployeeReturns404() throws Exception {
        asAdmin(get("/api/employees/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found with id 999999"));
    }

    @Test
    void listIsPagedWithTotals() throws Exception {
        for (int i = 0; i < 7; i++) {
            createEmployee(request("Employee " + (char) ('A' + i), "e" + i + "@example.com",
                    "12345678901" + i, "ABCDE123" + i + "F", IT));
        }
        asAdmin(get("/api/employees").param("pageNumber", "2").param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.pageNumber").value(2))
                .andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    /** Many requests for the same department at once must still get distinct, gap-free codes. */
    @Test
    void concurrentCreatesGetUniqueCodes() throws Exception {
        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<EmployeeResponse>> tasks = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int n = i;
                tasks.add(() -> employeeService.create(request("Parallel " + (char) ('A' + n),
                        "parallel" + n + "@example.com", "99999999900" + n, "PQRST123" + n + "Z", IT)));
            }
            Set<String> codes = new java.util.HashSet<>();
            for (Future<EmployeeResponse> future : pool.invokeAll(tasks)) {
                codes.add(future.get().employeeCode());
            }
            assertThat(codes).containsExactlyInAnyOrderElementsOf(
                    java.util.stream.IntStream.rangeClosed(1, threads)
                            .mapToObj(i -> String.format("I%04d", i)).collect(Collectors.toSet()));
        } finally {
            pool.shutdown();
        }
    }

    // ------------------------------------------------------------------ helpers

    private static EmployeeRequest request(String name, String email, String aadhaar, String pan, long departmentId) {
        return new EmployeeRequest(name, email, "9876543210", aadhaar, pan, LocalDate.of(1995, 4, 12),
                Gender.MALE, "12 MG Road, Bengaluru", departmentId, EMPLOYEE_ROLE, LocalDate.of(2021, 6, 1),
                new BigDecimal("650000"), null, null);
    }

    private static EmployeeRequest withDates(EmployeeRequest r, LocalDate dateOfBirth, LocalDate joiningDate) {
        return new EmployeeRequest(r.fullName(), r.email(), r.mobile(), r.aadhaarNumber(), r.panNumber(),
                dateOfBirth, r.gender(), r.address(), r.departmentId(), r.roleId(), joiningDate,
                r.annualSalary(), r.status(), r.version());
    }

    private ResultActions createEmployee(EmployeeRequest request) throws Exception {
        return asAdmin(post("/api/employees").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private long createAndGetId(EmployeeRequest request) throws Exception {
        String json = createEmployee(request).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(json).get("id").asLong();
    }
}
