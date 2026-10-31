# Employee Management System

Full Stack Developer Intern technical assessment: employee CRUD with personal details, department-wise
employee codes, department transfers with history, server-side search / filters / sorting / pagination,
validation on both ends, and JWT authentication with role-based authorization.

| Layer | Technology |
|---|---|
| Frontend | React 19, Vite 8, React Router 7, Axios |
| Backend | Java 17, Spring Boot 3.5.16 (Web, Data JPA / Hibernate, Validation, Security), JJWT, Maven (wrapper included) |
| Database | MySQL 8+ (developed and tested on MySQL 9.0) |
| API docs | Swagger UI (springdoc-openapi), OpenAPI JSON, Postman collection |

## Contents

1. [Project status](#project-status)
2. [Project structure](#project-structure)
3. [Setup and run](#setup-and-run)
4. [Demo accounts and permissions](#demo-accounts-and-permissions)
5. [REST API](#rest-api)
6. [Design decisions](#design-decisions)
7. [Validation rules](#validation-rules)
8. [Assumptions](#assumptions)
9. [Testing](#testing)
10. [Dependencies](#dependencies)
11. [Submission ZIP](#submission-zip)

## Project status

| Area | Status |
|---|---|
| Database schema, keys, constraints, indexes, reference data | ✅ Implemented & verified |
| Employee CRUD + soft delete (deactivate) | ✅ Implemented & tested |
| Department-wise employee code generation (concurrency-safe) | ✅ Implemented & tested |
| Department transfer + transfer history (transactional) | ✅ Implemented & tested |
| Server-side global search, 6 filters, sorting, pagination | ✅ Implemented & tested |
| Validation (frontend + backend), error handling, HTTP status codes | ✅ Implemented & tested |
| JWT authentication, roles ADMIN / MANAGER / EMPLOYEE | ✅ Implemented & tested |
| Aadhaar / PAN encryption at rest, masking, excluded from search | ✅ Implemented & tested |
| React frontend (list, filters, form, details, transfer, login) | ✅ Implemented & checked end-to-end in a browser |
| Sample data: 5 departments, 3 roles, 20 employees, 4 transfers, 3 users | ✅ Inserted automatically |
| Swagger UI, OpenAPI JSON, Postman collection | ✅ Included |

## Project structure

```
├── backend/                      Spring Boot REST API
│   ├── pom.xml, mvnw, mvnw.cmd   Maven build (wrapper: no Maven install needed)
│   └── src/main/java/com/ems/
│       ├── controller/           HTTP endpoints + @PreAuthorize access rules
│       ├── service/              Business logic, transactions, code generation, transfers
│       ├── repository/           Spring Data JPA repositories + search Specifications
│       ├── entity/               JPA entities and enums
│       ├── dto/                  Request / response records
│       ├── mapper/               Entity -> DTO, Aadhaar / PAN masking
│       ├── security/             JWT service + filter, access rules, AES-GCM / HMAC crypto
│       ├── exception/            Global exception handler, error response
│       ├── config/               Spring Security, CORS, OpenAPI
│       ├── validation/           Validation groups
│       └── seed/                 Sample data loader
├── frontend/                     React (Vite) single-page app
│   ├── package.json, package-lock.json
│   └── src/
│       ├── api/                  Axios client + API functions
│       ├── auth/                 Auth state, route guard
│       ├── components/           Layout, pagination, form field, toasts, badges
│       ├── hooks/                Lookups cache, debounce
│       ├── pages/                Login, list, form (add/edit), details, transfer
│       └── utils/                Formatting, validation rules, salary ranges
├── database/
│   ├── 01_schema.sql             All tables, keys, constraints, indexes
│   └── 02_seed_data.sql          Departments, roles, code sequences
├── docs/
│   ├── EmployeeManagement.postman_collection.json
│   └── openapi.json              Exported OpenAPI 3 specification
└── scripts/create-submission-zip.ps1
```

## Setup and run

### Prerequisites

- Java 17 or newer (`java -version`)
- Node.js 20 or newer (`node -v`)
- MySQL 8 or newer running on `localhost:3306`
- Maven is **not** required: the project includes the Maven Wrapper (`mvnw` / `mvnw.cmd`)

### 1. Database

```bash
mysql -u root -p < database/01_schema.sql
mysql -u root -p EMPLOYEEMNG < database/02_seed_data.sql
```

`01_schema.sql` creates the `EMPLOYEEMNG` database and all 7 tables. `02_seed_data.sql` inserts the
5 departments, 3 roles (Admin, Manager, Employee) and the employee-code sequences. Both scripts are safe to re-run.

### 2. Backend configuration (secrets stay out of Git)

Copy `backend/src/main/resources/application-local.properties.example` to
`backend/src/main/resources/application-local.properties` (this file is git-ignored) and fill it in:

```properties
DB_USERNAME=root
DB_PASSWORD=your_mysql_password
ENCRYPTION_KEY=...   # AES-256 key for Aadhaar / PAN
HASH_KEY=...         # HMAC key for duplicate detection
JWT_SECRET=...       # JWT signing key
```

Generate each of the three keys separately with:

```bash
node -e "console.log(require('crypto').randomBytes(32).toString('base64'))"
```

Instead of the file you can set the same names as environment variables (plus optional `DB_URL` and
`CORS_ALLOWED_ORIGINS`). Keep `ENCRYPTION_KEY` safe: stored Aadhaar / PAN values can't be read without it.

### 3. Run the backend

```bash
cd backend
mvnw.cmd spring-boot:run        # Windows
./mvnw spring-boot:run          # macOS / Linux
```

- API: http://localhost:9998/api
- Swagger UI: http://localhost:9998/swagger-ui.html (click **Authorize** and paste a token from `POST /api/auth/login`)

> **Sample data is inserted when the backend starts for the first time.** The SQL scripts only add
> departments, roles and code sequences. Right after importing them the `employees` table is still empty.
> That's on purpose: Aadhaar / PAN must be encrypted with *your* `ENCRYPTION_KEY` and passwords
> BCrypt-hashed, which only the application can do.

On the first start (log line `Inserted 20 sample employees`) the backend adds:

- **20 employees** across all 5 departments, with different roles, salaries (₹1.5 L – ₹18 L), genders and
  statuses (Active / On Leave / Inactive)
- **4 department transfers** with history (Imran Khan was transferred twice)
- **3 demo logins**: `admin`, `manager`, `rahul` (see [Demo accounts](#demo-accounts-and-permissions))

Each part is inserted only if its table is empty, so restarts never duplicate data. `02_seed_data.sql` must
be run first; without departments the backend logs a warning and skips the sample data. Set
`app.seed.enabled=false` to turn seeding off.

**Accessing the sample data:**

- **UI:** open http://localhost:5173 and log in as `admin` / `Admin@123` (all demo accounts are listed in
  [Demo accounts](#demo-accounts-and-permissions)). The employee list shows all 20 employees.
- **API:** Swagger UI or the Postman collection: log in with `POST /api/auth/login`, then call `GET /api/employees`.
- **Database:** `SELECT employee_code, full_name, status FROM EMPLOYEEMNG.employees;` and
  `SELECT * FROM EMPLOYEEMNG.employee_transfer_history;` (the Aadhaar / PAN columns hold only ciphertext).

### 4. Run the frontend

```bash
cd frontend
npm ci          # installs the exact versions from package-lock.json
npm run dev
```

Open http://localhost:5173 (Vite picks the next free port if 5173 is taken). The dev server forwards
`/api` to the backend on port 9998, so no extra configuration is needed.

Production build: `npm run build` (output in `frontend/dist`).

## Demo accounts and permissions

| Username | Password | Role | Linked employee |
|---|---|---|---|
| `admin` | `Admin@123` | ADMIN | — |
| `manager` | `Manager@123` | MANAGER | — |
| `rahul` | `Employee@123` | EMPLOYEE | Rahul Sharma (I0001) |

These are sample credentials for local testing only.

| Action | ADMIN | MANAGER | EMPLOYEE |
|---|:-:|:-:|:-:|
| Search / list employees | ✅ | ✅ | ❌ |
| View employee details | ✅ | ✅ | own record only |
| View full Aadhaar / PAN | ✅ | masked | own record only |
| Create / edit employees | ✅ | ✅ | ❌ |
| Transfer employees | ✅ | ✅ | ❌ |
| View transfer history | ✅ | ✅ | own record only |
| Deactivate employees | ✅ | ❌ | ❌ |

The backend enforces these rules (`@PreAuthorize`). The frontend only hides what a role can't use.

## REST API

All endpoints except login and the API docs need `Authorization: Bearer <token>`.

| Method | Endpoint | Description | Success |
|---|---|---|---|
| POST | `/api/auth/login` | Log in, returns JWT and user info | 200 |
| GET | `/api/auth/me` | Current user from the token | 200 |
| GET | `/api/employees` | Search, filter, sort, paginate | 200 |
| POST | `/api/employees` | Create employee (code generated) | 201 + `Location` |
| GET | `/api/employees/{id}` | Employment details | 200 |
| GET | `/api/employees/{id}/personal-details` | Aadhaar, PAN, date of birth | 200 |
| PUT | `/api/employees/{id}` | Update employee (not department) | 200 |
| PATCH | `/api/employees/{id}/deactivate` | Soft delete (status INACTIVE) | 200 |
| POST | `/api/employees/{id}/transfer` | Department transfer, new code | 200 |
| GET | `/api/employees/{id}/transfers` | Transfer history, newest first | 200 |
| GET | `/api/departments` | Departments | 200 |
| GET | `/api/roles` | Roles / designations | 200 |

**Error responses** share one format:

```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Validation failed",
  "path": "/api/employees", "fieldErrors": { "panNumber": "PAN must be in the format ABCDE1234F" } }
```

| Status | When |
|---|---|
| 400 | Validation error, invalid parameter, transfer to the same department, department change via edit |
| 401 | Missing / invalid / expired token, wrong login |
| 403 | Role not allowed, or an employee accessing another employee's record |
| 404 | Employee or endpoint not found |
| 409 | Duplicate email / Aadhaar / PAN, stale update (edited by someone else), transferring an inactive employee |

### Search parameters (`GET /api/employees`)

All optional, combined with AND:

| Parameter | Meaning |
|---|---|
| `search` | Case-insensitive text in code, name, email, mobile, address, department, role, status (never Aadhaar / PAN) |
| `departmentId`, `roleId` | Exact match |
| `status` | `ACTIVE`, `INACTIVE`, `ON_LEAVE` |
| `gender` | `MALE`, `FEMALE`, `OTHER` |
| `joinedFrom`, `joinedTo` | Joining date range `yyyy-MM-dd`, both inclusive |
| `minSalary`, `maxSalary` | `minSalary` inclusive, `maxSalary` **exclusive** (₹3–5 LPA = `300000`–`500000`) |
| `sortBy` | `id`, `employeeCode`, `fullName`, `email`, `mobile`, `departmentName`, `roleName`, `joiningDate`, `annualSalary`, `status` |
| `sortDir` | `asc` / `desc` |
| `pageNumber`, `pageSize` | 1-based page; 1–100 per page (the UI offers 5 / 10 / 20) |

Example from the assignment (returns Rahul Sharma from the sample data):
`GET /api/employees?search=Rahul&departmentId=1&minSalary=300000&maxSalary=500000&pageNumber=1&pageSize=10`

Response: `{ content: [...], pageNumber, pageSize, totalElements, totalPages }`

### API documentation

- **Swagger UI:** http://localhost:9998/swagger-ui.html (live, try requests in the browser)
- **OpenAPI JSON:** [docs/openapi.json](docs/openapi.json)
- **Postman:** import [docs/EmployeeManagement.postman_collection.json](docs/EmployeeManagement.postman_collection.json),
  run **Auth → Login as Admin** first (it stores the token), then any request

## Design decisions

**Employee codes.** Each department has a prefix (`departments.code_prefix`: I, H, F, S, A) and a counter
row in `employee_code_sequences`.
- **No duplicates:** to allocate a code, the service reads the counter with `SELECT … FOR UPDATE`, formats
  the next value (`I0007`) and increments it, all in the same transaction as the employee insert. Concurrent
  requests for the same department wait for each other.
- **Never reused:** codes from a failed insert are rolled back with it, and a transferred employee's old
  code stays retired.
- **Safety net:** a `UNIQUE` constraint on `employee_code`.

**Department transfer.** One `@Transactional` method:
1. Lock the employee row.
2. Validate: not the same department, not inactive, date not in the future, not before joining or the
   previous transfer.
3. Allocate the next code of the new department.
4. Update department and code. The internal `id` never changes.
5. Insert a history row with old and new department and code, the date, the reason and the user.

Any failure rolls everything back, including the counter. Tests prove both the rollback and the
concurrency handling.

**Department changes only via transfer.** Editing an employee can't change the department, otherwise the
code and the history would no longer match.

**Sensitive data.**
- **Separate table:** Aadhaar, PAN and date of birth live in `employee_personal_details`, apart from the
  searchable `employees` table.
- **Encrypted at rest:** Aadhaar and PAN are encrypted with AES-256-GCM through a JPA converter.
- **Duplicates without plain values:** a keyed HMAC-SHA256 hash column has a `UNIQUE` index, so the
  database rejects duplicates without ever storing the plain number.
- **Masked responses:** API responses show `XXXX-XXXX-9012` / `XXXXXX234F` unless the caller is ADMIN or
  the employee themself.
- **Never searched:** global search can't reach them.
- **Never logged:** values are never logged, and SQL logging shows only `?` placeholders.

**Search.** JPA Specifications add one predicate per supplied filter, so every combination of
search + filters + sort + page runs as one SQL query plus one count query.
- **Safe input:** `%` and `_` in the search text are escaped, and sorting is restricted to an allow-list.
- **No N+1 queries:** an entity graph loads department and role in the same query.

**Security.**
- **Stateless JWT:** HS256, 60 minutes, with user id, role and employee id in the claims. Passwords are
  stored as BCrypt hashes.
- **No user enumeration:** login returns the same error for an unknown user and a wrong password.
- **CORS:** limited to the frontend origin.
- **No CSRF needed:** with no cookies or sessions there's nothing for CSRF to exploit.

**Other.**
- **Optimistic locking:** `@Version` makes a stale edit return 409 instead of silently overwriting.
- **Soft delete:** deactivating keeps history and references intact.
- **Indexes:** on every filter and sort column.

## Validation rules

Applied in the React form (immediate feedback) **and** in the backend (Jakarta Bean Validation + service checks):

| Field | Rule |
|---|---|
| Full name | Required, alphabets and spaces only, max 100 |
| Email | Required, valid format, unique (case-insensitive) |
| Mobile | Exactly 10 digits |
| Aadhaar | Exactly 12 digits, shown and auto-formatted as `XXXX-XXXX-XXXX`, unique |
| PAN | `ABCDE1234F` format (auto upper-case), unique |
| Date of birth | Required, not in the future, employee at least 18 |
| Joining date | Required, on or after the 18th birthday |
| Annual salary | Required, greater than zero, max 2 decimals |
| Gender, address, department, role, status | Required |
| Transfer | New department required and different, date not in the future and not before joining, reason required (max 500) |

## Assumptions

- **Minimum age:** employees must be at least 18. The assignment doesn't state this.
- **Salary ranges:** `minSalary` is inclusive and `maxSalary` exclusive for both presets and the custom range.
- **Codes are never reused** after a transfer, matching the assignment's example (Rahul gets H0002, not H0001).
- **Roles:** exactly 3 roles, Admin, Manager and Employee, as required by the assignment. Two places use them:
  - The `roles` table holds the role of each employee record (shown, filtered, sorted and searched).
  - The login role in `users.user_role` (ADMIN / MANAGER / EMPLOYEE) decides permissions. An employee's role in
    the `roles` table grants no permissions on its own; access depends only on the login account.
- **Edits:** Aadhaar / PAN may be left blank when editing to keep the stored values.
- **Inactive employees can't be transferred.** Employees on leave can.
- **Status search:** searching by status matches word starts ("active" finds Active but not Inactive).

## Testing

```bash
cd backend
mvnw.cmd test          # Windows   (./mvnw test on macOS / Linux)
```

**45 automated tests**, run against a real MySQL database `EMPLOYEEMNG_TEST`. It's created automatically
with the same credentials, and your development data is never touched.

| Test class | What it covers |
|---|---|
| `EmployeeApiIntegrationTest` (12) | Create / read / update / deactivate, department codes, encrypted storage, masking, duplicate email and Aadhaar, field validation, minimum age, department change blocked, stale update 409, 404, pagination, 8 concurrent creates get unique codes |
| `EmployeeSearchIntegrationTest` (10) | Search across all permitted fields, status search, Aadhaar / PAN not searchable, `%` / `_` literal, salary-range boundaries, each filter, filters + sort + page together, invalid parameters |
| `EmployeeTransferIntegrationTest` (9) | Code change with unchanged id (assignment example), codes never reused, history order, same department, inactive employee, date rules, **rollback of a failed transfer**, concurrent transfers, role restriction |
| `SecurityIntegrationTest` (8) | 401 without / with tampered token, login success / failure, employee sees only own record, manager masking and no deactivate (neither PATCH nor a status change via PUT), admin full access, inactive employee can't log in |
| `AesGcmEncryptorTest`, `SensitiveDataHasherTest` (6) | Encryption round-trip, random IV, tamper detection, key length, hashing |

The frontend was checked end-to-end in a browser (Microsoft Edge): login, search / filters / sorting /
pagination, form validation and auto-formatting, create, duplicate email, edit, transfer, deactivate,
masking for managers, employee restrictions, and phone-width layout.

## Dependencies

### Backend (Maven, `backend/pom.xml`)

| Dependency | Version | Purpose |
|---|---|---|
| spring-boot-starter-parent | 3.5.16 | Dependency management, build plugins |
| spring-boot-starter-web | 3.5.16 | REST controllers, embedded Tomcat 10.1.55 |
| spring-boot-starter-data-jpa | 3.5.16 | Spring Data JPA, Hibernate 6.6.53 |
| spring-boot-starter-validation | 3.5.16 | Jakarta Bean Validation |
| spring-boot-starter-security | 3.5.16 | Spring Security 6.5.11, BCrypt |
| io.jsonwebtoken:jjwt-api / jjwt-impl / jjwt-jackson | 0.12.6 | JWT creation and verification |
| com.mysql:mysql-connector-j | 9.7.0 (managed by Spring Boot) | MySQL JDBC driver |
| org.springdoc:springdoc-openapi-starter-webmvc-ui | 2.8.17 | Swagger UI / OpenAPI |
| spring-boot-starter-test | 3.5.16 (test) | JUnit 5, MockMvc, AssertJ |

Restore and build: `cd backend && mvnw.cmd clean package` (downloads Maven 3.9.11 and all dependencies;
add `-DskipTests` to skip the tests).

### Frontend (npm, `frontend/package.json` + `package-lock.json`)

`package.json` declares version ranges; `package-lock.json` pins the exact versions that `npm ci` installs.

| Package | Declared (`package.json`) | Installed (`package-lock.json`) | Purpose |
|---|---|---|---|
| react | ^19.3.1 | 19.3.1 | UI library |
| react-dom | ^19.3.1 | 19.3.1 | UI library (DOM renderer) |
| react-router-dom | ^7.18.5 | 7.18.5 | Routing |
| axios | ^1.21.0 | 1.21.0 | HTTP client |
| vite (dev) | ^8.3.5 | 8.3.5 | Dev server and build |
| @vitejs/plugin-react (dev) | ^6.1.2 | 6.1.2 | React support for Vite |
| oxlint (dev) | ^1.89.0 | 1.90.0 | Linting (`npm run lint`) |
| @types/react (dev) | ^19.3.0 | 19.3.0 | React type definitions (editor IntelliSense; the project is JavaScript) |
| @types/react-dom (dev) | ^19.3.0 | 19.3.0 | React DOM type definitions (editor IntelliSense) |

Restore: `cd frontend && npm ci` (exact versions from the lockfile).

## Submission ZIP

```powershell
powershell -ExecutionPolicy Bypass -File scripts/create-submission-zip.ps1
```

Creates `EmployeeManagementSystem.zip` with the frontend, backend, database scripts and docs. It
excludes `node_modules`, `dist`, `target`, `.git`, IDE folders and `application-local.properties` /
`.env` files, so there are no credentials and no build output.
