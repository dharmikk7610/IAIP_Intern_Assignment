package com.ems.seed;

import com.ems.dto.EmployeeRequest;
import com.ems.dto.TransferRequest;
import com.ems.entity.Employee;
import com.ems.entity.EmployeeStatus;
import com.ems.entity.Gender;
import com.ems.entity.User;
import com.ems.entity.UserRole;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.EmployeeTransferHistoryRepository;
import com.ems.repository.UserRepository;
import com.ems.service.EmployeeService;
import com.ems.service.EmployeeTransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.ems.entity.EmployeeStatus.ACTIVE;
import static com.ems.entity.EmployeeStatus.INACTIVE;
import static com.ems.entity.EmployeeStatus.ON_LEAVE;
import static com.ems.entity.Gender.FEMALE;
import static com.ems.entity.Gender.MALE;
import static com.ems.entity.Gender.OTHER;

/**
 * Inserts sample employees, department transfers and demo users on start-up.
 * Goes through EmployeeService so codes, validation and encryption are exactly
 * what a real request would produce. Disable with app.seed.enabled=false.
 * Department / role ids match database/02_seed_data.sql.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class SampleDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SampleDataSeeder.class);

    private static final long IT = 1, HR = 2, FINANCE = 3, SALES = 4, ADMIN = 5;
    private static final long SOFTWARE_ENGINEER = 1, SENIOR_SOFTWARE_ENGINEER = 2, TEAM_LEAD = 3,
            HR_EXECUTIVE = 4, HR_MANAGER = 5, ACCOUNTANT = 6, FINANCE_MANAGER = 7,
            SALES_EXECUTIVE = 8, SALES_MANAGER = 9, OFFICE_ADMINISTRATOR = 10;

    private record Sample(String name, Gender gender, long departmentId, long roleId, String joiningDate,
                          long salary, EmployeeStatus status, String dateOfBirth, String city) {
    }

    private static final List<Sample> SAMPLES = List.of(
            new Sample("Rahul Sharma", MALE, IT, SOFTWARE_ENGINEER, "2021-06-01", 450000, ACTIVE, "1995-04-12", "Bengaluru"),
            new Sample("Priya Nair", FEMALE, IT, SENIOR_SOFTWARE_ENGINEER, "2018-03-15", 1250000, ACTIVE, "1990-08-21", "Kochi"),
            new Sample("Arjun Mehta", MALE, IT, TEAM_LEAD, "2016-07-11", 1800000, ACTIVE, "1987-01-30", "Pune"),
            new Sample("Sneha Kulkarni", FEMALE, IT, SOFTWARE_ENGINEER, "2023-01-09", 420000, ACTIVE, "1999-11-05", "Pune"),
            new Sample("Vikram Singh", MALE, IT, SOFTWARE_ENGINEER, "2024-02-19", 280000, ON_LEAVE, "2001-06-17", "Jaipur"),
            new Sample("Ananya Iyer", FEMALE, IT, SENIOR_SOFTWARE_ENGINEER, "2019-09-23", 950000, INACTIVE, "1992-03-08", "Chennai"),
            new Sample("Kavita Joshi", FEMALE, HR, HR_MANAGER, "2017-05-02", 1100000, ACTIVE, "1986-12-14", "Mumbai"),
            new Sample("Rohan Desai", MALE, HR, HR_EXECUTIVE, "2022-08-16", 350000, ACTIVE, "1997-07-25", "Ahmedabad"),
            new Sample("Meera Pillai", FEMALE, HR, HR_EXECUTIVE, "2024-06-03", 240000, ACTIVE, "2002-02-11", "Thiruvananthapuram"),
            new Sample("Sanjay Gupta", MALE, HR, HR_EXECUTIVE, "2020-11-30", 310000, INACTIVE, "1994-09-19", "Lucknow"),
            new Sample("Amit Verma", MALE, FINANCE, FINANCE_MANAGER, "2015-04-20", 1500000, ACTIVE, "1984-05-03", "Delhi"),
            new Sample("Pooja Reddy", FEMALE, FINANCE, ACCOUNTANT, "2021-10-04", 480000, ACTIVE, "1996-10-27", "Hyderabad"),
            new Sample("Karthik Rao", MALE, FINANCE, ACCOUNTANT, "2022-03-14", 360000, ON_LEAVE, "1995-01-16", "Mysuru"),
            new Sample("Neha Bansal", FEMALE, FINANCE, ACCOUNTANT, "2025-01-06", 190000, ACTIVE, "2003-04-09", "Chandigarh"),
            new Sample("Suresh Patil", MALE, SALES, SALES_MANAGER, "2016-12-01", 1050000, ACTIVE, "1985-08-28", "Nagpur"),
            new Sample("Divya Menon", FEMALE, SALES, SALES_EXECUTIVE, "2022-07-18", 380000, ACTIVE, "1998-06-02", "Kozhikode"),
            new Sample("Imran Khan", MALE, SALES, SALES_EXECUTIVE, "2023-09-25", 260000, ACTIVE, "2000-12-22", "Bhopal"),
            new Sample("Lakshmi Narayan", FEMALE, SALES, SALES_EXECUTIVE, "2019-02-11", 550000, INACTIVE, "1993-03-15", "Coimbatore"),
            new Sample("Aditya Chauhan", MALE, SALES, SALES_EXECUTIVE, "2025-03-17", 150000, ACTIVE, "2003-09-01", "Indore"),
            new Sample("Ritu Agarwal", FEMALE, ADMIN, OFFICE_ADMINISTRATOR, "2020-01-13", 450000, ACTIVE, "1991-07-07", "Kolkata"),
            new Sample("Manoj Tiwari", MALE, ADMIN, OFFICE_ADMINISTRATOR, "2023-04-10", 220000, ACTIVE, "1999-02-18", "Patna"),
            new Sample("Alex Fernandes", OTHER, ADMIN, OFFICE_ADMINISTRATOR, "2018-08-27", 720000, ON_LEAVE, "1989-10-10", "Panaji")
    );

    /** Sample transfers, applied in this order. Imran moves twice to show a multi-step history. */
    private record SampleTransfer(String email, long newDepartmentId, String date, String reason) {
    }

    private static final List<SampleTransfer> TRANSFERS = List.of(
            new SampleTransfer("ritu.agarwal@example.com", FINANCE, "2023-07-01",
                    "Moved to finance operations to support vendor payments"),
            new SampleTransfer("divya.menon@example.com", HR, "2024-04-01",
                    "Internal move to HR as talent acquisition executive"),
            new SampleTransfer("imran.khan@example.com", IT, "2024-10-15",
                    "Joined IT pre-sales engineering team"),
            new SampleTransfer("imran.khan@example.com", SALES, "2025-06-01",
                    "Returned to sales after pre-sales project ended"));

    /** Demo logins (documented in the README). The EMPLOYEE login belongs to Rahul Sharma. */
    private static final String ADMIN_PASSWORD = "Admin@123";
    private static final String MANAGER_PASSWORD = "Manager@123";
    private static final String EMPLOYEE_PASSWORD = "Employee@123";

    private final EmployeeService employeeService;
    private final EmployeeTransferService transferService;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final EmployeeTransferHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SampleDataSeeder(EmployeeService employeeService, EmployeeTransferService transferService,
                            EmployeeRepository employeeRepository, DepartmentRepository departmentRepository,
                            EmployeeTransferHistoryRepository historyRepository, UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
        this.employeeService = employeeService;
        this.transferService = transferService;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.historyRepository = historyRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Each part runs only if its table is still empty, so restarts never duplicate data. */
    @Override
    public void run(ApplicationArguments args) {
        if (departmentRepository.count() == 0) {
            log.warn("Skipping sample data: no departments found. Run database/02_seed_data.sql first.");
            return;
        }
        if (employeeRepository.count() == 0) {
            for (int i = 0; i < SAMPLES.size(); i++) {
                employeeService.create(toRequest(SAMPLES.get(i), i + 1));
            }
            log.info("Inserted {} sample employees", SAMPLES.size());
        }
        if (historyRepository.count() == 0) {
            seedTransfers();
        }
        if (userRepository.count() == 0) {
            seedUsers();
        }
    }

    private void seedTransfers() {
        int applied = 0;
        for (SampleTransfer t : TRANSFERS) {
            var employee = employeeRepository.findByEmail(t.email());
            if (employee.isPresent()) {
                transferService.transfer(employee.get().getId(),
                        new TransferRequest(t.newDepartmentId(), LocalDate.parse(t.date()), t.reason()), null);
                applied++;
            }
        }
        log.info("Inserted {} sample department transfers", applied);
    }

    private void seedUsers() {
        createUser("admin", ADMIN_PASSWORD, UserRole.ADMIN, null);
        createUser("manager", MANAGER_PASSWORD, UserRole.MANAGER, null);
        employeeRepository.findByEmail("rahul.sharma@example.com")
                .ifPresent(rahul -> createUser("rahul", EMPLOYEE_PASSWORD, UserRole.EMPLOYEE, rahul));
        log.info("Inserted demo users: admin, manager, rahul (see README for passwords)");
    }

    private void createUser(String username, String password, UserRole role, Employee employee) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setUserRole(role);
        user.setEmployee(employee);
        userRepository.save(user);
    }

    /** Builds unique, format-valid (but fictitious) contact and identity numbers from the index. */
    private static EmployeeRequest toRequest(Sample s, int n) {
        String[] nameParts = s.name().split(" ");
        String email = (nameParts[0] + "." + nameParts[1] + "@example.com").toLowerCase();
        String mobile = "98765" + String.format("%05d", 43200 + n);
        String aadhaar = "4321" + String.format("%08d", 10000000 + n * 7919);
        String pan = "ABCP" + nameParts[1].charAt(0) + String.format("%04d", 1000 + n) + "K";
        return new EmployeeRequest(s.name(), email, mobile, aadhaar, pan, LocalDate.parse(s.dateOfBirth()),
                s.gender(), n + ", " + s.city() + " Main Road, " + s.city(), s.departmentId(), s.roleId(),
                LocalDate.parse(s.joiningDate()), BigDecimal.valueOf(s.salary()), s.status(), null);
    }
}
