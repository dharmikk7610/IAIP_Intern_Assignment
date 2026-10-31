package com.ems;

import com.ems.dto.EmployeeRequest;
import com.ems.entity.EmployeeStatus;
import com.ems.entity.Gender;
import com.ems.service.EmployeeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private EmployeeService employeeService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void requestsWithoutValidTokenGet401() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
        mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
        // signature check: change one character of a real token
        String tampered = adminToken.substring(0, adminToken.length() - 2) + "xx";
        mockMvc.perform(get("/api/employees").header(HttpHeaders.AUTHORIZATION, tampered))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginReturnsUsableTokenAndRejectsWrongPassword() throws Exception {
        String json = mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyString())))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn().getResponse().getContentAsString();
        String token = new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("token").asText();

        as("Bearer " + token, get("/api/auth/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"nobody\",\"password\":\"whatever\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void employeeCanOnlySeeOwnRecordWithFullSensitiveData() throws Exception {
        long rahul = create("Rahul Sharma", 1);
        long priya = create("Priya Nair", 2);
        String rahulToken = employeeToken(rahul);

        as(rahulToken, get("/api/employees/{id}", rahul)).andExpect(status().isOk());
        as(rahulToken, get("/api/employees/{id}/personal-details", rahul))
                .andExpect(jsonPath("$.aadhaarNumber").value("5555-0000-0001"))
                .andExpect(jsonPath("$.masked").value(false));
        as(rahulToken, get("/api/employees/{id}/transfers", rahul)).andExpect(status().isOk());

        as(rahulToken, get("/api/employees/{id}", priya)).andExpect(status().isForbidden());
        as(rahulToken, get("/api/employees/{id}/personal-details", priya)).andExpect(status().isForbidden());
        as(rahulToken, get("/api/employees")).andExpect(status().isForbidden());
        as(rahulToken, patch("/api/employees/{id}/deactivate", rahul)).andExpect(status().isForbidden());
    }

    @Test
    void managerSeesMaskedDataAndCannotDeactivate() throws Exception {
        long rahul = create("Rahul Sharma", 1);

        as(managerToken, get("/api/employees")).andExpect(status().isOk());
        as(managerToken, get("/api/employees/{id}/personal-details", rahul))
                .andExpect(jsonPath("$.aadhaarNumber").value("XXXX-XXXX-0001"))
                .andExpect(jsonPath("$.panNumber").value("XXXXXX001F"))
                .andExpect(jsonPath("$.masked").value(true));
        as(managerToken, patch("/api/employees/{id}/deactivate", rahul))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void adminSeesFullSensitiveDataAndCanDeactivate() throws Exception {
        long rahul = create("Rahul Sharma", 1);

        asAdmin(get("/api/employees/{id}/personal-details", rahul))
                .andExpect(jsonPath("$.aadhaarNumber").value("5555-0000-0001"))
                .andExpect(jsonPath("$.panNumber").value("ABCDE0001F"))
                .andExpect(jsonPath("$.masked").value(false));
        asAdmin(patch("/api/employees/{id}/deactivate", rahul)).andExpect(status().isOk());
    }

    @Test
    void managerCannotDeactivateOrReactivateThroughUpdate() throws Exception {
        long rahul = create("Rahul Sharma", 1);

        // Deactivating through PUT is refused and nothing is saved
        as(managerToken, put("/api/employees/{id}", rahul).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("Changed address", EmployeeStatus.INACTIVE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
        asAdmin(get("/api/employees/{id}", rahul))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.address").value("Bengaluru"));

        // Editing other fields keeps the status; ACTIVE <-> ON_LEAVE stays allowed
        as(managerToken, put("/api/employees/{id}", rahul).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("New address", EmployeeStatus.ACTIVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.address").value("New address"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        as(managerToken, put("/api/employees/{id}", rahul).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("New address", EmployeeStatus.ON_LEAVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ON_LEAVE"));

        // Reactivating is ADMIN-only too
        asAdmin(patch("/api/employees/{id}/deactivate", rahul)).andExpect(status().isOk());
        as(managerToken, put("/api/employees/{id}", rahul).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("New address", EmployeeStatus.ACTIVE)))
                .andExpect(status().isForbidden());
        as(managerToken, put("/api/employees/{id}", rahul).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("Inactive but edited", EmployeeStatus.INACTIVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        // ADMIN keeps full control through PUT
        asAdmin(put("/api/employees/{id}", rahul).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("Inactive but edited", EmployeeStatus.ACTIVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        asAdmin(put("/api/employees/{id}", rahul).contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody("Inactive but edited", EmployeeStatus.INACTIVE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void inactiveEmployeeCannotLogIn() throws Exception {
        long rahul = create("Rahul Sharma", 1);
        employeeToken(rahul); // creates login "emp<id>" / Employee@123 linked to Rahul
        String credentials = "{\"username\":\"emp" + rahul + "\",\"password\":\"Employee@123\"}";

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("EMPLOYEE"));

        asAdmin(patch("/api/employees/{id}/deactivate", rahul)).andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(credentials))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.token").doesNotExist());

        // ADMIN and MANAGER logins (no linked employee) are unaffected
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"manager\",\"password\":\"Manager@123\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void swaggerDocsArePublic() throws Exception {
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    private long create(String name, int n) {
        return employeeService.create(new EmployeeRequest(name, "user" + n + "@example.com", "9876543210",
                String.format("5555%08d", n), String.format("ABCDE%04dF", n), LocalDate.of(1995, 4, 12),
                Gender.MALE, "Bengaluru", 1L, 3L, LocalDate.of(2021, 6, 1), new BigDecimal("450000"),
                null, null)).id();
    }

    /** PUT body for the employee made by create(.., 1), with only address and status varied. */
    private String updateBody(String address, EmployeeStatus status) throws Exception {
        return objectMapper.writeValueAsString(new EmployeeRequest("Rahul Sharma", "user1@example.com",
                "9876543210", null, null, LocalDate.of(1995, 4, 12), Gender.MALE, address, 1L, 3L,
                LocalDate.of(2021, 6, 1), new BigDecimal("450000"), status, null));
    }
}
