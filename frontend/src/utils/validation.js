import { addYears, todayIso } from './format'

// Same rules as the backend (EmployeeRequest / EmployeeService), so users see errors before submitting.
export const PATTERNS = {
  name: /^[A-Za-z ]+$/,
  email: /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/,
  mobile: /^\d{10}$/,
  aadhaar: /^\d{4}-\d{4}-\d{4}$/,
  pan: /^[A-Z]{5}\d{4}[A-Z]$/,
  salary: /^\d+(\.\d{1,2})?$/,
}

const MINIMUM_AGE = 18

/** Formats as the user types: "123456789012" -> "1234-5678-9012" (max 12 digits). */
export function formatAadhaarInput(value) {
  const digits = value.replace(/\D/g, '').slice(0, 12)
  return digits.replace(/(\d{4})(?=\d)/g, '$1-')
}

/** Upper-case letters and digits only, max 10 characters. */
export function formatPanInput(value) {
  return value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 10)
}

/** Digits only, max 10. */
export function formatMobileInput(value) {
  return value.replace(/\D/g, '').slice(0, 10)
}

/**
 * Returns { field: message } for every invalid field (empty object = valid).
 * On edit, Aadhaar / PAN may be left blank to keep the stored values.
 */
export function validateEmployee(v, { isEdit = false } = {}) {
  const errors = {}
  const today = todayIso()

  const name = v.fullName.trim()
  if (!name) errors.fullName = 'Full name is required'
  else if (!PATTERNS.name.test(name)) errors.fullName = 'Full name may contain only alphabets and spaces'
  else if (name.length > 100) errors.fullName = 'Full name must be at most 100 characters'

  const email = v.email.trim()
  if (!email) errors.email = 'Email is required'
  else if (!PATTERNS.email.test(email)) errors.email = 'Enter a valid email address'

  if (!v.mobile) errors.mobile = 'Mobile number is required'
  else if (!PATTERNS.mobile.test(v.mobile)) errors.mobile = 'Mobile number must be exactly 10 digits'

  if (!isEdit || v.aadhaarNumber) {
    if (!v.aadhaarNumber) errors.aadhaarNumber = 'Aadhaar number is required'
    else if (!PATTERNS.aadhaar.test(v.aadhaarNumber)) errors.aadhaarNumber = 'Aadhaar must be exactly 12 digits'
  }

  if (!isEdit || v.panNumber) {
    if (!v.panNumber) errors.panNumber = 'PAN is required'
    else if (!PATTERNS.pan.test(v.panNumber)) errors.panNumber = 'PAN must be in the format ABCDE1234F'
  }

  if (!v.dateOfBirth) errors.dateOfBirth = 'Date of birth is required'
  else if (v.dateOfBirth > today) errors.dateOfBirth = 'Date of birth cannot be a future date'
  else if (addYears(v.dateOfBirth, MINIMUM_AGE) > today) errors.dateOfBirth = 'Employee must be at least 18 years old'

  if (!v.gender) errors.gender = 'Gender is required'

  if (!v.address.trim()) errors.address = 'Address is required'
  else if (v.address.length > 500) errors.address = 'Address must be at most 500 characters'

  if (!v.departmentId) errors.departmentId = 'Department is required'
  if (!v.roleId) errors.roleId = 'Role / designation is required'

  if (!v.joiningDate) errors.joiningDate = 'Joining date is required'
  else if (v.dateOfBirth && !errors.dateOfBirth && v.joiningDate < addYears(v.dateOfBirth, MINIMUM_AGE)) {
    errors.joiningDate = "Joining date must be on or after the employee's 18th birthday"
  }

  const salary = String(v.annualSalary).trim()
  if (!salary) errors.annualSalary = 'Annual salary is required'
  else if (!PATTERNS.salary.test(salary)) errors.annualSalary = 'Enter a number with at most 2 decimals'
  else if (Number(salary) <= 0) errors.annualSalary = 'Annual salary must be greater than zero'
  else if (Number(salary) >= 1e10) errors.annualSalary = 'Annual salary is too large'

  if (!v.status) errors.status = 'Status is required'

  return errors
}

export function validateTransfer(v, { currentDepartmentId, joiningDate }) {
  const errors = {}
  if (!v.newDepartmentId) errors.newDepartmentId = 'New department is required'
  else if (Number(v.newDepartmentId) === currentDepartmentId) {
    errors.newDepartmentId = 'Choose a department different from the current one'
  }
  if (!v.transferDate) errors.transferDate = 'Transfer date is required'
  else if (v.transferDate > todayIso()) errors.transferDate = 'Transfer date cannot be in the future'
  else if (joiningDate && v.transferDate < joiningDate) errors.transferDate = 'Transfer date cannot be before the joining date'
  if (!v.reason.trim()) errors.reason = 'Transfer reason is required'
  else if (v.reason.length > 500) errors.reason = 'Transfer reason must be at most 500 characters'
  return errors
}
