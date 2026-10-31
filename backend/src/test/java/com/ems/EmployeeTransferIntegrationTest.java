package com.ems;

import com.ems.dto.EmployeeRequest;
import com.ems.dto.TransferRequest;
import com.ems.dto.TransferResponse;
import com.ems.entity.EmployeeStatus;
import com.ems.entity.Gender;
import com.ems.service.EmployeeService;
import com.ems.service.EmployeeTransferService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployeeTransferIntegrationTest extends AbstractIntegrationTest {

    private static final long IT = 1, HR = 2, FINANCE = 3, EMPLOYEE_ROLE = 3;

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private EmployeeTransferService transferService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** The assignment's example: Rahul (IT, I0001) moves to HR, which already has H0001 -> H0002. */
    @Test
    void transferGeneratesNextCodeOfNewDepartmentAndKeepsId() throws Exception {
        long rahul = create("Rahul Sharma", 1, IT, EmployeeStatus.ACTIVE);
        create("Kavita Joshi", 2, HR, EmployeeStatus.ACTIVE);

        transfer(rahul, HR, LocalDate.now(), "Moving to HR")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.id").value(rahul))
                .andExpect(jsonPath("$.employee.employeeCode").value("H0002"))
                .andExpect(jsonPath("$.employee.departmentName").value("HR"))
                .andExpect(jsonPath("$.transfer.oldEmployeeCode").value("I0001"))
                .andExpect(jsonPath("$.transfer.newEmployeeCode").value("H0002"))
                .andExpect(jsonPath("$.transfer.transferredBy").value("admin"));

        asAdmin(get("/api/employees/{id}", rahul))
                .andExpect(jsonPath("$.employeeCode").value("H0002"))
                .andExpect(jsonPath("$.departmentId").value(HR));
        asAdmin(get("/api/employees/{id}/personal-details", rahul))
                .andExpect(jsonPath("$.employeeCode").value("H0002"));
        asAdmin(get("/api/employees/{id}/transfers", rahul))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].oldDepartmentName").value("IT"))
                .andExpect(jsonPath("$[0].newDepartmentName").value("HR"))
                .andExpect(jsonPath("$[0].reason").value("Moving to HR"));
    }

    @Test
    void codesAreNeverReusedAndHistoryIsKeptInOrder() throws Exception {
        long rahul = create("Rahul Sharma", 1, IT, EmployeeStatus.ACTIVE);
        LocalDate today = LocalDate.now();

        transfer(rahul, HR, today.minusDays(10), "To HR").andExpect(status().isOk());
        transfer(rahul, IT, today, "Back to IT")
                .andExpect(jsonPath("$.employee.employeeCode").value("I0002")); // I0001 is retired

        asAdmin(get("/api/employees/{id}/transfers", rahul))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].newEmployeeCode").value("I0002"))
                .andExpect(jsonPath("$[1].newEmployeeCode").value("H0001"));
    }

    @Test
    void transferToSameDepartmentIsRejected() throws Exception {
        long rahul = create("Rahul Sharma", 1, IT, EmployeeStatus.ACTIVE);

        transfer(rahul, IT, LocalDate.now(), "No-op")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.newDepartmentId").exists());
    }

    @Test
    void inactiveEmployeeCannotBeTransferred() throws Exception {
        long rahul = create("Rahul Sharma", 1, IT, EmployeeStatus.INACTIVE);

        transfer(rahul, HR, LocalDate.now(), "Should fail").andExpect(status().isConflict());
    }

    @Test
    void invalidTransferDatesAndMissingReasonAreRejected() throws Exception {
        long rahul = create("Rahul Sharma", 1, IT, EmployeeStatus.ACTIVE); // joined 2021-06-01

        transfer(rahul, HR, LocalDate.of(2020, 1, 1), "Before joining")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.transferDate").exists());
        transfer(rahul, HR, LocalDate.now().plusDays(1), "Future")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.transferDate").exists());
        transfer(rahul, HR, LocalDate.now(), " ")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.reason").exists());

        transfer(rahul, HR, LocalDate.now(), "Valid").andExpect(status().isOk());
        transfer(rahul, FINANCE, LocalDate.now().minusDays(1), "Before previous transfer")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.transferDate").exists());
    }

    @Test
    void unknownEmployeeReturns404() throws Exception {
        transfer(999999, HR, LocalDate.now(), "Nobody").andExpect(status().isNotFound());
    }

    /**
     * Force a failure AFTER the new code was allocated (reason longer than the DB column,
     * calling the service directly to skip request validation). Nothing may be saved:
     * employee unchanged, HR sequence not advanced, no history row.
     */
    @Test
    void failedTransferRollsBackEverything() {
        long rahul = create("Rahul Sharma", 1, IT, EmployeeStatus.ACTIVE);
        String tooLongReason = "x".repeat(600);

        assertThatThrownBy(() -> transferService.transfer(rahul,
                new TransferRequest(HR, LocalDate.now(), tooLongReason), null))
                .isInstanceOf(Exception.class);

        assertThat(jdbcTemplate.queryForObject("SELECT employee_code FROM employees WHERE id = ?", String.class, rahul))
                .isEqualTo("I0001");
        assertThat(jdbcTemplate.queryForObject("SELECT department_id FROM employees WHERE id = ?", Long.class, rahul))
                .isEqualTo(IT);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT next_value FROM employee_code_sequences WHERE department_id = ?", Integer.class, HR))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM employee_transfer_history", Integer.class))
                .isZero();
    }

    @Test
    void concurrentTransfersIntoSameDepartmentGetUniqueCodes() throws Exception {
        int threads = 6;
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            ids.add(create("Person " + (char) ('A' + i), 10 + i, IT, EmployeeStatus.ACTIVE));
        }
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Callable<TransferResponse>> tasks = new ArrayList<>();
            for (long id : ids) {
                tasks.add(() -> transferService.transfer(id, new TransferRequest(HR, LocalDate.now(), "Bulk move"), null));
            }
            Set<String> codes = new HashSet<>();
            for (Future<TransferResponse> f : pool.invokeAll(tasks)) {
                codes.add(f.get().employee().employeeCode());
            }
            assertThat(codes).containsExactlyInAnyOrder("H0001", "H0002", "H0003", "H0004", "H0005", "H0006");
        } finally {
            pool.shutdown();
        }
    }

    @Test
    void onlyAdminAndManagerCanTransfer() throws Exception {
        long rahul = create("Rahul Sharma", 1, IT, EmployeeStatus.ACTIVE);
        String body = objectMapper.writeValueAsString(new TransferRequest(HR, LocalDate.now(), "Self transfer"));

        as(employeeToken(rahul), post("/api/employees/{id}/transfer", rahul)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        as(managerToken, post("/api/employees/{id}/transfer", rahul)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transfer.transferredBy").value("manager"));
    }

    // ------------------------------------------------------------------ helpers

    private long create(String name, int n, long departmentId, EmployeeStatus status) {
        return employeeService.create(new EmployeeRequest(name, "person" + n + "@example.com", "9876543210",
                String.format("5555%08d", n), String.format("ABCDE%04dF", n), LocalDate.of(1995, 4, 12),
                Gender.MALE, "Bengaluru", departmentId, EMPLOYEE_ROLE, LocalDate.of(2021, 6, 1),
                new BigDecimal("450000"), status, null)).id();
    }

    private ResultActions transfer(long employeeId, long newDepartmentId, LocalDate date, String reason)
            throws Exception {
        return asAdmin(post("/api/employees/{id}/transfer", employeeId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TransferRequest(newDepartmentId, date, reason))));
    }
}
