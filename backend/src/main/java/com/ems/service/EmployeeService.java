package com.ems.service;

import com.ems.dto.EmployeePersonalDetailsResponse;
import com.ems.dto.EmployeeRequest;
import com.ems.dto.EmployeeResponse;
import com.ems.dto.EmployeeSearchCriteria;
import com.ems.dto.PageResponse;
import com.ems.entity.Department;
import com.ems.entity.Employee;
import com.ems.entity.EmployeePersonalDetails;
import com.ems.entity.EmployeeStatus;
import com.ems.entity.Role;
import com.ems.exception.BusinessValidationException;
import com.ems.exception.ConflictException;
import com.ems.exception.ResourceNotFoundException;
import com.ems.mapper.EmployeeMapper;
import com.ems.repository.DepartmentRepository;
import com.ems.repository.EmployeePersonalDetailsRepository;
import com.ems.repository.EmployeeRepository;
import com.ems.repository.EmployeeSpecifications;
import com.ems.repository.RoleRepository;
import com.ems.security.crypto.SensitiveDataHasher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class EmployeeService {

    private static final int MINIMUM_AGE_YEARS = 18;
    private static final int MAX_PAGE_SIZE = 100;

    /** API sort name -> entity property path. Anything else is rejected. */
    private static final Map<String, String> SORTABLE_FIELDS = orderedMap(
            "id", "id",
            "employeeCode", "employeeCode",
            "fullName", "fullName",
            "email", "email",
            "mobile", "mobile",
            "departmentName", "department.name",
            "roleName", "role.name",
            "joiningDate", "joiningDate",
            "annualSalary", "annualSalary",
            "status", "status");

    private final EmployeeRepository employeeRepository;
    private final EmployeePersonalDetailsRepository personalDetailsRepository;
    private final DepartmentRepository departmentRepository;
    private final RoleRepository roleRepository;
    private final EmployeeCodeGenerator codeGenerator;
    private final SensitiveDataHasher hasher;
    private final EmployeeMapper mapper;

    public EmployeeService(EmployeeRepository employeeRepository,
                           EmployeePersonalDetailsRepository personalDetailsRepository,
                           DepartmentRepository departmentRepository,
                           RoleRepository roleRepository,
                           EmployeeCodeGenerator codeGenerator,
                           SensitiveDataHasher hasher,
                           EmployeeMapper mapper) {
        this.employeeRepository = employeeRepository;
        this.personalDetailsRepository = personalDetailsRepository;
        this.departmentRepository = departmentRepository;
        this.roleRepository = roleRepository;
        this.codeGenerator = codeGenerator;
        this.hasher = hasher;
        this.mapper = mapper;
    }

    /**
     * One transaction: lock the department's code sequence, insert the employee and the
     * personal details. Any failure rolls everything back, including the sequence increment.
     */
    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        validateDates(request);
        Department department = findDepartment(request.departmentId());
        Role role = findRole(request.roleId());

        String email = normalizeEmail(request.email());
        String aadhaar = normalizeAadhaar(request.aadhaarNumber());
        String pan = normalizePan(request.panNumber());
        String aadhaarHash = hasher.hash(aadhaar);
        String panHash = hasher.hash(pan);

        if (employeeRepository.existsByEmail(email)) {
            throw new ConflictException("email", "An employee with this email already exists");
        }
        if (personalDetailsRepository.existsByAadhaarHash(aadhaarHash)) {
            throw new ConflictException("aadhaarNumber", "An employee with this Aadhaar number already exists");
        }
        if (personalDetailsRepository.existsByPanHash(panHash)) {
            throw new ConflictException("panNumber", "An employee with this PAN already exists");
        }

        Employee employee = new Employee();
        applyEmploymentFields(employee, request, email, role);
        employee.setDepartment(department);
        employee.setStatus(request.status() != null ? request.status() : EmployeeStatus.ACTIVE);
        employee.setEmployeeCode(codeGenerator.nextCodeFor(department));
        employeeRepository.save(employee);

        EmployeePersonalDetails details = new EmployeePersonalDetails();
        details.setEmployee(employee);
        details.setAadhaarNumber(aadhaar);
        details.setAadhaarHash(aadhaarHash);
        details.setPanNumber(pan);
        details.setPanHash(panHash);
        details.setDateOfBirth(request.dateOfBirth());
        personalDetailsRepository.save(details);

        employeeRepository.flush(); // populate timestamps / version for the response
        return mapper.toResponse(employee);
    }

    /**
     * Server-side search + filters + sorting + pagination in one query (plus one COUNT query
     * for the total). See {@link EmployeeSpecifications} for how filters are combined.
     */
    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> search(EmployeeSearchCriteria criteria) {
        validateSearchCriteria(criteria);
        Sort sort = toSort(criteria.sortBy(), criteria.sortDir());
        PageRequest page = PageRequest.of(criteria.pageNumber() - 1, criteria.pageSize(), sort);
        return PageResponse.from(employeeRepository
                .findAll(EmployeeSpecifications.matching(criteria), page)
                .map(mapper::toResponse));
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return mapper.toResponse(findEmployee(id));
    }

    @Transactional(readOnly = true)
    public EmployeePersonalDetailsResponse getPersonalDetails(Long id, boolean revealSensitive) {
        Employee employee = findEmployee(id);
        return mapper.toPersonalDetailsResponse(employee, findPersonalDetails(id), revealSensitive);
    }

    /**
     * Updates everything except the department and employee code (those change only through
     * a transfer). Aadhaar / PAN are updated only when supplied.
     *
     * @param mayChangeActivation true for ADMIN: only they may move an employee into or out of INACTIVE
     */
    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request, boolean mayChangeActivation) {
        Employee employee = findEmployee(id);
        if (request.version() != null && !request.version().equals(employee.getVersion())) {
            throw new ConflictException("This employee was modified by someone else. Reload and try again.");
        }
        if (!mayChangeActivation && request.status() != null
                && (request.status() == EmployeeStatus.INACTIVE) != (employee.getStatus() == EmployeeStatus.INACTIVE)) {
            throw new AccessDeniedException("Only an Admin can deactivate or reactivate an employee");
        }
        if (!employee.getDepartment().getId().equals(request.departmentId())) {
            throw new BusinessValidationException("departmentId",
                    "Department cannot be changed here. Use the department transfer instead.");
        }
        validateDates(request);
        Role role = findRole(request.roleId());

        String email = normalizeEmail(request.email());
        if (employeeRepository.existsByEmailAndIdNot(email, id)) {
            throw new ConflictException("email", "An employee with this email already exists");
        }

        EmployeePersonalDetails details = findPersonalDetails(id);
        if (request.aadhaarNumber() != null && !request.aadhaarNumber().isBlank()) {
            String aadhaar = normalizeAadhaar(request.aadhaarNumber());
            String aadhaarHash = hasher.hash(aadhaar);
            if (personalDetailsRepository.existsByAadhaarHashAndEmployeeIdNot(aadhaarHash, id)) {
                throw new ConflictException("aadhaarNumber", "An employee with this Aadhaar number already exists");
            }
            details.setAadhaarNumber(aadhaar);
            details.setAadhaarHash(aadhaarHash);
        }
        if (request.panNumber() != null && !request.panNumber().isBlank()) {
            String pan = normalizePan(request.panNumber());
            String panHash = hasher.hash(pan);
            if (personalDetailsRepository.existsByPanHashAndEmployeeIdNot(panHash, id)) {
                throw new ConflictException("panNumber", "An employee with this PAN already exists");
            }
            details.setPanNumber(pan);
            details.setPanHash(panHash);
        }
        details.setDateOfBirth(request.dateOfBirth());

        applyEmploymentFields(employee, request, email, role);
        if (request.status() != null) {
            employee.setStatus(request.status());
        }
        employeeRepository.flush(); // fail here (not after the response) if the version is stale
        return mapper.toResponse(employee);
    }

    /** Soft delete: the record and its transfer history are kept. Repeating it is harmless. */
    @Transactional
    public EmployeeResponse deactivate(Long id) {
        Employee employee = findEmployee(id);
        employee.setStatus(EmployeeStatus.INACTIVE);
        employeeRepository.flush();
        return mapper.toResponse(employee);
    }

    // ------------------------------------------------------------------ helpers

    private static void validateSearchCriteria(EmployeeSearchCriteria c) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (c.pageNumber() < 1) {
            errors.put("pageNumber", "Page number must be 1 or greater");
        }
        if (c.pageSize() < 1 || c.pageSize() > MAX_PAGE_SIZE) {
            errors.put("pageSize", "Page size must be between 1 and " + MAX_PAGE_SIZE);
        }
        if (c.sortBy() != null && !SORTABLE_FIELDS.containsKey(c.sortBy())) {
            errors.put("sortBy", "Sorting is allowed by: " + String.join(", ", SORTABLE_FIELDS.keySet()));
        }
        if (c.sortDir() != null && !c.sortDir().equalsIgnoreCase("asc") && !c.sortDir().equalsIgnoreCase("desc")) {
            errors.put("sortDir", "Sort direction must be asc or desc");
        }
        if (c.minSalary() != null && c.minSalary().signum() < 0) {
            errors.put("minSalary", "Minimum salary cannot be negative");
        }
        if (c.minSalary() != null && c.maxSalary() != null && c.minSalary().compareTo(c.maxSalary()) > 0) {
            errors.put("maxSalary", "Maximum salary must be greater than or equal to minimum salary");
        }
        if (c.joinedFrom() != null && c.joinedTo() != null && c.joinedFrom().isAfter(c.joinedTo())) {
            errors.put("joinedTo", "'Joined to' date must be on or after 'joined from' date");
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }
    }

    /** Only allow-listed fields can be sorted on; id is a tie-breaker so paging is stable. */
    private static Sort toSort(String sortBy, String sortDir) {
        Sort.Direction direction = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        if (sortBy == null || sortBy.equals("id")) {
            return Sort.by(direction, "id");
        }
        return Sort.by(direction, SORTABLE_FIELDS.get(sortBy)).and(Sort.by(Sort.Direction.ASC, "id"));
    }

    private void applyEmploymentFields(Employee employee, EmployeeRequest request, String email, Role role) {
        employee.setFullName(normalizeName(request.fullName()));
        employee.setEmail(email);
        employee.setMobile(request.mobile().trim());
        employee.setGender(request.gender());
        employee.setAddress(request.address().trim());
        employee.setRole(role);
        employee.setJoiningDate(request.joiningDate());
        employee.setAnnualSalary(request.annualSalary());
    }

    /** Rules involving two fields or today's date, which annotations cannot express. */
    private void validateDates(EmployeeRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        LocalDate eighteenthBirthday = request.dateOfBirth().plusYears(MINIMUM_AGE_YEARS);
        if (eighteenthBirthday.isAfter(LocalDate.now())) {
            errors.put("dateOfBirth", "Employee must be at least " + MINIMUM_AGE_YEARS + " years old");
        } else if (request.joiningDate().isBefore(eighteenthBirthday)) {
            errors.put("joiningDate", "Joining date must be on or after the employee's 18th birthday");
        }
        if (!errors.isEmpty()) {
            throw new BusinessValidationException(errors);
        }
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findWithDepartmentAndRoleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    private EmployeePersonalDetails findPersonalDetails(Long employeeId) {
        return personalDetailsRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Personal details not found for employee id " + employeeId));
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("departmentId", "Department does not exist"));
    }

    private Role findRole(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new BusinessValidationException("roleId", "Role / designation does not exist"));
    }

    private static Map<String, String> orderedMap(String... keyValues) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put(keyValues[i], keyValues[i + 1]);
        }
        return java.util.Collections.unmodifiableMap(map);
    }

    /** "  rahul   sharma " -> "rahul sharma" */
    private static String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** "1234-5678-9012" -> "123456789012" */
    private static String normalizeAadhaar(String aadhaar) {
        return aadhaar.replace("-", "").trim();
    }

    private static String normalizePan(String pan) {
        return pan.trim().toUpperCase(Locale.ROOT);
    }
}
