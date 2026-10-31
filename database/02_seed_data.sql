-- =====================================================================
-- Reference data: 5 departments, 3 roles, code sequences.
-- Safe to run more than once (INSERT IGNORE).
--   mysql -u root -p EMPLOYEEMNG < database/02_seed_data.sql
--
-- Sample employees, users and transfer history are inserted by the
-- backend on first start-up (when the employees table is empty), because
-- Aadhaar/PAN must be encrypted with YOUR key and passwords BCrypt-hashed.
-- (No USE statement: the target database is chosen on the command line,
--  which lets the automated tests load this same file into a test database.)
-- =====================================================================

INSERT IGNORE INTO departments (id, name, code_prefix) VALUES
    (1, 'IT',      'I'),
    (2, 'HR',      'H'),
    (3, 'Finance', 'F'),
    (4, 'Sales',   'S'),
    (5, 'Admin',   'A');

-- The three roles required by the assignment. An employee's role here is data only;
-- permissions come from the login account (users.user_role).
INSERT IGNORE INTO roles (id, name) VALUES
    (1, 'Admin'),
    (2, 'Manager'),
    (3, 'Employee');

-- next_value = number the NEXT employee in that department will get
INSERT IGNORE INTO employee_code_sequences (department_id, next_value) VALUES
    (1, 1),
    (2, 1),
    (3, 1),
    (4, 1),
    (5, 1);
