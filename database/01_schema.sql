-- =====================================================================
-- Employee Management System - Database schema (MySQL 8+)
-- Run once:  mysql -u root -p < database/01_schema.sql
--
-- Notes
--  * InnoDB engine (default) -> transactions + foreign keys.
--  * utf8mb4_0900_ai_ci collation is case-insensitive, so the UNIQUE email
--    constraint and LIKE searches ignore letter case.
--  * Aadhaar / PAN are stored AES-GCM encrypted by the application.
--    The *_hash columns (HMAC-SHA256) let the DB enforce uniqueness without
--    ever storing the plain value.
-- =====================================================================

CREATE DATABASE IF NOT EXISTS EMPLOYEEMNG
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE EMPLOYEEMNG;

-- ---------------------------------------------------------------------
-- Departments: code_prefix drives employee codes (IT -> I0001)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS departments (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)  NOT NULL,
    code_prefix VARCHAR(1)   NOT NULL,
    CONSTRAINT pk_departments PRIMARY KEY (id),
    CONSTRAINT uk_departments_name UNIQUE (name),
    CONSTRAINT uk_departments_code_prefix UNIQUE (code_prefix)
);

-- ---------------------------------------------------------------------
-- Roles = job designations (Software Engineer, HR Executive, ...).
-- Login roles (ADMIN/MANAGER/EMPLOYEE) live in users.user_role.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id   BIGINT      NOT NULL AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL,
    CONSTRAINT pk_roles PRIMARY KEY (id),
    CONSTRAINT uk_roles_name UNIQUE (name)
);

-- ---------------------------------------------------------------------
-- Employees: general + employment information (searchable fields)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employees (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    employee_code VARCHAR(10)   NOT NULL,
    full_name     VARCHAR(100)  NOT NULL,
    email         VARCHAR(150)  NOT NULL,
    mobile        VARCHAR(10)   NOT NULL,
    gender        VARCHAR(10)   NOT NULL,
    address       VARCHAR(500)  NOT NULL,
    department_id BIGINT        NOT NULL,
    role_id       BIGINT        NOT NULL,
    joining_date  DATE          NOT NULL,
    annual_salary DECIMAL(12,2) NOT NULL,
    status        VARCHAR(20)   NOT NULL,
    version       BIGINT        NOT NULL DEFAULT 0,
    created_at    DATETIME(6)   NOT NULL,
    updated_at    DATETIME(6)   NOT NULL,
    CONSTRAINT pk_employees PRIMARY KEY (id),
    CONSTRAINT uk_employees_code  UNIQUE (employee_code),
    CONSTRAINT uk_employees_email UNIQUE (email),
    CONSTRAINT chk_employees_salary CHECK (annual_salary > 0),
    INDEX idx_employees_department   (department_id),
    INDEX idx_employees_role         (role_id),
    INDEX idx_employees_status       (status),
    INDEX idx_employees_joining_date (joining_date),
    INDEX idx_employees_salary       (annual_salary),
    INDEX idx_employees_full_name    (full_name),
    CONSTRAINT fk_employees_department FOREIGN KEY (department_id) REFERENCES departments (id),
    CONSTRAINT fk_employees_role       FOREIGN KEY (role_id)       REFERENCES roles (id)
);

-- ---------------------------------------------------------------------
-- Sensitive personal details (1:1 with employees, same primary key)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employee_personal_details (
    employee_id       BIGINT       NOT NULL,
    aadhaar_encrypted VARCHAR(255) NOT NULL,
    aadhaar_hash      VARCHAR(64)  NOT NULL,
    pan_encrypted     VARCHAR(255) NOT NULL,
    pan_hash          VARCHAR(64)  NOT NULL,
    date_of_birth     DATE         NOT NULL,
    CONSTRAINT pk_employee_personal_details PRIMARY KEY (employee_id),
    CONSTRAINT uk_personal_aadhaar_hash UNIQUE (aadhaar_hash),
    CONSTRAINT uk_personal_pan_hash     UNIQUE (pan_hash),
    CONSTRAINT fk_personal_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

-- ---------------------------------------------------------------------
-- One row per department; locked with SELECT ... FOR UPDATE when a code
-- is allocated, so two concurrent requests can never get the same code.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employee_code_sequences (
    department_id BIGINT NOT NULL,
    next_value    INT    NOT NULL,
    CONSTRAINT pk_employee_code_sequences PRIMARY KEY (department_id),
    CONSTRAINT fk_code_seq_department FOREIGN KEY (department_id) REFERENCES departments (id)
);

-- ---------------------------------------------------------------------
-- Application users (login). employee_id links an EMPLOYEE login to
-- their own record; ADMIN/MANAGER logins may have no employee record.
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    user_role     VARCHAR(20)  NOT NULL,
    employee_id   BIGINT       NULL,
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME(6)  NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_employee UNIQUE (employee_id),
    CONSTRAINT fk_users_employee FOREIGN KEY (employee_id) REFERENCES employees (id)
);

-- ---------------------------------------------------------------------
-- Complete transfer history (old/new department and code)
-- ---------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS employee_transfer_history (
    id                     BIGINT       NOT NULL AUTO_INCREMENT,
    employee_id            BIGINT       NOT NULL,
    old_department_id      BIGINT       NOT NULL,
    new_department_id      BIGINT       NOT NULL,
    old_employee_code      VARCHAR(10)  NOT NULL,
    new_employee_code      VARCHAR(10)  NOT NULL,
    transfer_date          DATE         NOT NULL,
    reason                 VARCHAR(500) NOT NULL,
    transferred_by_user_id BIGINT       NULL,
    created_at             DATETIME(6)  NOT NULL,
    CONSTRAINT pk_employee_transfer_history PRIMARY KEY (id),
    INDEX idx_transfer_employee (employee_id),
    CONSTRAINT fk_transfer_employee       FOREIGN KEY (employee_id)            REFERENCES employees (id),
    CONSTRAINT fk_transfer_old_department FOREIGN KEY (old_department_id)      REFERENCES departments (id),
    CONSTRAINT fk_transfer_new_department FOREIGN KEY (new_department_id)      REFERENCES departments (id),
    CONSTRAINT fk_transfer_user           FOREIGN KEY (transferred_by_user_id) REFERENCES users (id)
);
