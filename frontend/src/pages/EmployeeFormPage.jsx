import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getErrorMessage, getFieldErrors } from '../api/client'
import { createEmployee, getEmployee, getPersonalDetails, updateEmployee } from '../api/employees'
import { useAuth } from '../auth/useAuth'
import FormField from '../components/FormField'
import { useToast } from '../components/toastContext'
import { useLookups } from '../hooks/useLookups'
import { GENDER_OPTIONS, STATUS_OPTIONS, todayIso } from '../utils/format'
import {
  formatAadhaarInput,
  formatMobileInput,
  formatPanInput,
  validateEmployee,
} from '../utils/validation'

const EMPTY_FORM = {
  fullName: '',
  email: '',
  mobile: '',
  aadhaarNumber: '',
  panNumber: '',
  dateOfBirth: '',
  gender: '',
  address: '',
  departmentId: '',
  roleId: '',
  joiningDate: '',
  annualSalary: '',
  status: 'ACTIVE',
}

// Inputs that are reformatted while typing
const FORMATTERS = {
  mobile: formatMobileInput,
  aadhaarNumber: formatAadhaarInput,
  panNumber: formatPanInput,
}

/** Registration form (/employees/new) and edit form (/employees/:id/edit). */
export default function EmployeeFormPage() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()
  const toast = useToast()
  const { departments, roles } = useLookups()
  const { hasRole } = useAuth()

  const [values, setValues] = useState(EMPTY_FORM)
  const [errors, setErrors] = useState({})
  const [touched, setTouched] = useState({})
  const [existing, setExisting] = useState(null) // { employee, personal } when editing
  const [loading, setLoading] = useState(isEdit)
  const [loadError, setLoadError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!isEdit) return undefined
    let active = true
    Promise.all([getEmployee(id), getPersonalDetails(id)])
      .then(([employee, personal]) => {
        if (!active) return
        setExisting({ employee, personal })
        setValues({
          fullName: employee.fullName,
          email: employee.email,
          mobile: employee.mobile,
          aadhaarNumber: '', // blank = keep the stored value
          panNumber: '',
          dateOfBirth: personal.dateOfBirth,
          gender: employee.gender,
          address: employee.address,
          departmentId: String(employee.departmentId),
          roleId: String(employee.roleId),
          joiningDate: employee.joiningDate,
          annualSalary: String(employee.annualSalary),
          status: employee.status,
        })
      })
      .catch((err) => active && setLoadError(getErrorMessage(err)))
      .finally(() => active && setLoading(false))
    return () => {
      active = false
    }
  }, [id, isEdit])

  const handleChange = (event) => {
    const { name, value } = event.target
    const formatted = FORMATTERS[name] ? FORMATTERS[name](value) : value
    const next = { ...values, [name]: formatted }
    setValues(next)
    if (touched[name]) {
      setErrors((prev) => ({ ...prev, [name]: validateEmployee(next, { isEdit })[name] }))
    }
  }

  const handleBlur = (event) => {
    const { name } = event.target
    setTouched((prev) => ({ ...prev, [name]: true }))
    setErrors((prev) => ({ ...prev, [name]: validateEmployee(values, { isEdit })[name] }))
  }

  const focusFirstError = (fieldErrors) => {
    const first = Object.keys(fieldErrors)[0]
    document.getElementById(`field-${first}`)?.focus()
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const validationErrors = validateEmployee(values, { isEdit })
    setErrors(validationErrors)
    setTouched(Object.fromEntries(Object.keys(EMPTY_FORM).map((k) => [k, true])))
    if (Object.keys(validationErrors).length > 0) {
      toast.error('Please correct the highlighted fields')
      focusFirstError(validationErrors)
      return
    }

    const payload = {
      ...values,
      fullName: values.fullName.trim().replace(/\s+/g, ' '),
      email: values.email.trim(),
      address: values.address.trim(),
      departmentId: Number(values.departmentId),
      roleId: Number(values.roleId),
      annualSalary: Number(values.annualSalary),
      aadhaarNumber: values.aadhaarNumber || null,
      panNumber: values.panNumber || null,
      version: existing?.employee.version ?? null,
    }

    setSubmitting(true)
    try {
      const saved = isEdit ? await updateEmployee(id, payload) : await createEmployee(payload)
      toast.success(
        isEdit
          ? `${saved.fullName} (${saved.employeeCode}) updated`
          : `${saved.fullName} added with employee code ${saved.employeeCode}`,
      )
      navigate(`/employees/${saved.id}`)
    } catch (err) {
      const fieldErrors = getFieldErrors(err)
      setErrors((prev) => ({ ...prev, ...fieldErrors }))
      toast.error(getErrorMessage(err))
      focusFirstError(fieldErrors)
      setSubmitting(false)
    }
  }

  if (loading) return <p className="muted">Loading employee…</p>
  if (loadError) return <p className="alert alert-error">{loadError}</p>

  const today = todayIso()
  // Moving into or out of INACTIVE is ADMIN-only (the API enforces the same rule)
  const lockActivation = isEdit && !hasRole('ADMIN')
  const fieldProps = (name) => ({ value: values[name], onChange: handleChange, onBlur: handleBlur })

  return (
    <section>
      <div className="page-header">
        <div>
          <h1>{isEdit ? 'Edit Employee' : 'Employee Registration'}</h1>
          <p className="muted">Fields marked * are required.</p>
        </div>
        <Link to={isEdit ? `/employees/${id}` : '/employees'} className="btn btn-ghost">
          Cancel
        </Link>
      </div>

      <form className="card form-card" onSubmit={handleSubmit} noValidate>
        <h2 className="section-title">Employment details</h2>
        <div className="form-grid">
          <FormField label="Employee Code" name="employeeCode"
            hint={isEdit ? 'Changes only through a department transfer' : 'Generated automatically from the department'}>
            <input value={isEdit ? existing.employee.employeeCode : 'Auto-generated'} readOnly disabled />
          </FormField>
          <FormField label="Department" name="departmentId" required error={errors.departmentId}
            hint={isEdit ? 'Use Transfer to move this employee to another department' : undefined}>
            <select {...fieldProps('departmentId')} disabled={isEdit}>
              <option value="">Select department</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>
                  {d.name} ({d.codePrefix}xxxx)
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Role / Designation" name="roleId" required error={errors.roleId}>
            <select {...fieldProps('roleId')}>
              <option value="">Select role</option>
              {roles.map((r) => (
                <option key={r.id} value={r.id}>
                  {r.name}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Joining Date" name="joiningDate" required error={errors.joiningDate}>
            <input type="date" {...fieldProps('joiningDate')} />
          </FormField>
          <FormField label="Annual Salary (₹)" name="annualSalary" required error={errors.annualSalary}>
            <input type="number" min="1" step="0.01" inputMode="decimal" placeholder="e.g. 450000"
              {...fieldProps('annualSalary')} />
          </FormField>
          <FormField label="Employee Status" name="status" required error={errors.status}
            hint={lockActivation ? 'Only an Admin can deactivate or reactivate an employee' : undefined}>
            <select {...fieldProps('status')}>
              {STATUS_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}
                  disabled={lockActivation && (o.value === 'INACTIVE') !== (existing.employee.status === 'INACTIVE')}>
                  {o.label}
                </option>
              ))}
            </select>
          </FormField>
        </div>

        <h2 className="section-title">Personal details</h2>
        <div className="form-grid">
          <FormField label="Full Name" name="fullName" required error={errors.fullName}>
            <input autoComplete="off" placeholder="e.g. Rahul Sharma" {...fieldProps('fullName')} />
          </FormField>
          <FormField label="Email Address" name="email" required error={errors.email}>
            <input type="email" autoComplete="off" placeholder="name@company.com" {...fieldProps('email')} />
          </FormField>
          <FormField label="Mobile Number" name="mobile" required error={errors.mobile}>
            <input inputMode="numeric" placeholder="10 digits" {...fieldProps('mobile')} />
          </FormField>
          <FormField label="Aadhaar Number" name="aadhaarNumber" required={!isEdit} error={errors.aadhaarNumber}
            hint={isEdit ? `Stored: ${existing.personal.aadhaarNumber}. Leave blank to keep it.` : 'Formatted automatically as XXXX-XXXX-XXXX'}>
            <input inputMode="numeric" autoComplete="off" placeholder="XXXX-XXXX-XXXX" {...fieldProps('aadhaarNumber')} />
          </FormField>
          <FormField label="PAN Number" name="panNumber" required={!isEdit} error={errors.panNumber}
            hint={isEdit ? `Stored: ${existing.personal.panNumber}. Leave blank to keep it.` : 'Format ABCDE1234F'}>
            <input autoComplete="off" placeholder="ABCDE1234F" {...fieldProps('panNumber')} />
          </FormField>
          <FormField label="Date of Birth" name="dateOfBirth" required error={errors.dateOfBirth}>
            <input type="date" max={today} {...fieldProps('dateOfBirth')} />
          </FormField>
          <FormField label="Gender" name="gender" required error={errors.gender}>
            <select {...fieldProps('gender')}>
              <option value="">Select gender</option>
              {GENDER_OPTIONS.map((o) => (
                <option key={o.value} value={o.value}>
                  {o.label}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Address" name="address" required error={errors.address} className="span-2">
            <textarea rows={3} maxLength={500} {...fieldProps('address')} />
          </FormField>
        </div>

        <div className="form-actions">
          <Link to={isEdit ? `/employees/${id}` : '/employees'} className="btn btn-ghost">
            Cancel
          </Link>
          <button type="submit" className="btn btn-primary" disabled={submitting}>
            {submitting ? 'Saving…' : isEdit ? 'Save changes' : 'Register employee'}
          </button>
        </div>
      </form>
    </section>
  )
}
