-- =====================================================================
-- Reference data: departments, roles (designations), code sequences.
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

INSERT IGNORE INTO roles (id, name) VALUES
    (1,  'Software Engineer'),
    (2,  'Senior Software Engineer'),
    (3,  'Team Lead'),
    (4,  'HR Executive'),
    (5,  'HR Manager'),
    (6,  'Accountant'),
    (7,  'Finance Manager'),
    (8,  'Sales Executive'),
    (9,  'Sales Manager'),
    (10, 'Office Administrator');

-- next_value = number the NEXT employee in that department will get
INSERT IGNORE INTO employee_code_sequences (department_id, next_value) VALUES
    (1, 1),
    (2, 1),
    (3, 1),
    (4, 1),
    (5, 1);
