package com.ems;

import com.ems.dto.EmployeeRequest;
import com.ems.entity.EmployeeStatus;
import com.ems.entity.Gender;
import com.ems.service.EmployeeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmployeeSearchIntegrationTest extends AbstractIntegrationTest {

    private static final long IT = 1, HR = 2, FINANCE = 3;
    private static final long MANAGER_ROLE = 2, EMPLOYEE_ROLE = 3;
    @Autowired
    private EmployeeService employeeService;

    /**
     * Salaries sit exactly on the LPA boundaries to prove min is inclusive and max exclusive.
     * I0001 Rahul  IT      300000   ACTIVE    M  2021-06-01
     * I0002 Priya  IT      500000   ON_LEAVE  F  2022-01-10
     * H0001 Kavita HR     1000000   ACTIVE    F  2019-03-01
     * H0002 Rohan  HR      299999   INACTIVE  M  2023-05-20
     * F0001 Amit   Finance 750000   ACTIVE    M  2020-07-15  (address contains "_")
     */
    @BeforeEach
    void createEmployees() {
        create("Rahul Sharma", "rahul.sharma@example.com", "9000000001", "111122223333", "AAAAA1111A",
                Gender.MALE, "MG Road, Bengaluru", IT, EMPLOYEE_ROLE, "2021-06-01", "300000", EmployeeStatus.ACTIVE);
        create("Priya Nair", "priya.nair@example.com", "9000000002", "111122224444", "AAAAA2222A",
                Gender.FEMALE, "Marine Drive, Kochi", IT, EMPLOYEE_ROLE, "2022-01-10", "500000",
                EmployeeStatus.ON_LEAVE);
        create("Kavita Joshi", "kavita.joshi@example.com", "9000000003", "111122225555", "AAAAA3333A",
                Gender.FEMALE, "FC Road, Pune", HR, MANAGER_ROLE, "2019-03-01", "1000000", EmployeeStatus.ACTIVE);
        create("Rohan Desai", "rohan.desai@example.com", "9000000004", "111122226666", "AAAAA4444A",
                Gender.MALE, "Andheri, Mumbai", HR, EMPLOYEE_ROLE, "2023-05-20", "299999", EmployeeStatus.INACTIVE);
        create("Amit Verma", "amit.verma@example.com", "9000000005", "111122227777", "AAAAA5555A",
                Gender.MALE, "Block_7, Delhi", FINANCE, MANAGER_ROLE, "2020-07-15", "750000", EmployeeStatus.ACTIVE);
    }

    @Test
    void searchByPartialNameIgnoresCase() throws Exception {
        search(get("/api/employees").param("search", "SHAR"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].employeeCode").value("I0001"));
    }

    @Test
    void searchCoversCodeEmailMobileAddressDepartmentAndRole() throws Exception {
        expectCodes("search", "H000", "H0001", "H0002");
        expectCodes("search", "priya.nair@", "I0002");
        expectCodes("search", "9000000004", "H0002");
        expectCodes("search", "pune", "H0001");
        expectCodes("search", "finance", "F0001");
        expectCodes("search", "manager", "H0001", "F0001");
    }

    @Test
    void searchMatchesStatusByWordStart() throws Exception {
        expectCodes("search", "leave", "I0002");
        expectCodes("search", "on leave", "I0002");
        // "active" must not also return INACTIVE employees
        expectCodes("search", "active", "I0001", "H0001", "F0001");
        expectCodes("search", "inactive", "H0002");
    }

    @Test
    void searchNeverMatchesAadhaarOrPan() throws Exception {
        search(get("/api/employees").param("search", "111122223333")).andExpect(jsonPath("$.totalElements").value(0));
        search(get("/api/employees").param("search", "AAAAA1111A")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void likeWildcardsAreTreatedAsPlainCharacters() throws Exception {
        search(get("/api/employees").param("search", "%")).andExpect(jsonPath("$.totalElements").value(0));
        expectCodes("search", "_", "F0001");
    }

    @Test
    void salaryPresetsUseInclusiveMinAndExclusiveMax() throws Exception {
        expectCodesForRange("100000", "300000", "H0002");          // 1-3 LPA: 299999 in, 300000 out
        expectCodesForRange("300000", "500000", "I0001");          // 3-5 LPA: 300000 in, 500000 out
        expectCodesForRange("500000", "1000000", "I0002", "F0001"); // 5-10 LPA
        expectCodesForRange("1000000", null, "H0001");             // 10+ LPA
    }

    @Test
    void individualFiltersWork() throws Exception {
        expectCodes("departmentId", String.valueOf(HR), "H0001", "H0002");
        expectCodes("roleId", String.valueOf(MANAGER_ROLE), "H0001", "F0001");
        expectCodes("status", "ON_LEAVE", "I0002");
        expectCodes("gender", "FEMALE", "I0002", "H0001");
        search(get("/api/employees").param("joinedFrom", "2021-01-01").param("joinedTo", "2022-01-10"))
                .andExpect(jsonPath("$.content[*].employeeCode", containsInAnyOrder("I0001", "I0002")));
    }

    @Test
    void searchFiltersSortingAndPaginationWorkTogether() throws Exception {
        // ACTIVE and salary >= 3 LPA -> Rahul 300000, Amit 750000, Kavita 1000000; sorted desc, 1 per page
        search(get("/api/employees").param("status", "ACTIVE").param("minSalary", "300000")
                .param("sortBy", "annualSalary").param("sortDir", "desc")
                .param("pageNumber", "2").param("pageSize", "1"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(3))
                .andExpect(jsonPath("$.pageNumber").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].employeeCode").value("F0001"));

        search(get("/api/employees").param("search", "a").param("departmentId", String.valueOf(IT))
                .param("gender", "FEMALE"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].employeeCode").value("I0002"));
    }

    @Test
    void sortsByJoinedFieldsAscendingAndDescending() throws Exception {
        search(get("/api/employees").param("sortBy", "departmentName").param("sortDir", "asc"))
                .andExpect(jsonPath("$.content[0].departmentName").value("Finance"));
        search(get("/api/employees").param("sortBy", "fullName").param("sortDir", "desc"))
                .andExpect(jsonPath("$.content[0].fullName").value("Rohan Desai"));
    }

    @Test
    void invalidSearchParametersReturn400() throws Exception {
        asAdmin(get("/api/employees").param("sortBy", "aadhaarNumber"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.sortBy").exists());
        asAdmin(get("/api/employees").param("sortDir", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.sortDir").exists());
        asAdmin(get("/api/employees").param("minSalary", "500000").param("maxSalary", "300000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.maxSalary").exists());
        asAdmin(get("/api/employees").param("status", "RETIRED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.status").exists());
        asAdmin(get("/api/employees").param("pageSize", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.pageSize").exists());
    }

    // ------------------------------------------------------------------ helpers

    private void create(String name, String email, String mobile, String aadhaar, String pan, Gender gender,
                        String address, long departmentId, long roleId, String joiningDate, String salary,
                        EmployeeStatus status) {
        employeeService.create(new EmployeeRequest(name, email, mobile, aadhaar, pan, LocalDate.of(1990, 1, 1),
                gender, address, departmentId, roleId, LocalDate.parse(joiningDate), new BigDecimal(salary),
                status, null));
    }

    /** Default page size (10) is larger than the 5 test employees, so totals and content match. */
    private ResultActions search(MockHttpServletRequestBuilder request) throws Exception {
        return asAdmin(request).andExpect(status().isOk());
    }

    private void expectCodes(String param, String value, String... codes) throws Exception {
        search(get("/api/employees").param(param, value))
                .andExpect(jsonPath("$.totalElements").value(codes.length))
                .andExpect(jsonPath("$.content[*].employeeCode", containsInAnyOrder(codes)));
    }

    private void expectCodesForRange(String min, String max, String... codes) throws Exception {
        MockHttpServletRequestBuilder request = get("/api/employees").param("minSalary", min);
        if (max != null) {
            request.param("maxSalary", max);
        }
        search(request)
                .andExpect(jsonPath("$.totalElements").value(codes.length))
                .andExpect(jsonPath("$.content[*].employeeCode", containsInAnyOrder(codes)));
    }
}
