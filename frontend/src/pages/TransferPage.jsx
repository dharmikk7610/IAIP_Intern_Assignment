import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { getErrorMessage, getFieldErrors } from '../api/client'
import { getEmployee, transferEmployee } from '../api/employees'
import FormField from '../components/FormField'
import { useToast } from '../components/toastContext'
import { useLookups } from '../hooks/useLookups'
import { todayIso } from '../utils/format'
import { validateTransfer } from '../utils/validation'

/** Employee Transfer form: moves an employee to another department and shows the new code. */
export default function TransferPage() {
  const { id } = useParams()
  const navigate = useNavigate()
  const toast = useToast()
  const { departments } = useLookups()

  const [employee, setEmployee] = useState(null)
  const [loadError, setLoadError] = useState('')
  const [values, setValues] = useState({ newDepartmentId: '', transferDate: todayIso(), reason: '' })
  const [errors, setErrors] = useState({})
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    let active = true
    getEmployee(id)
      .then((e) => active && setEmployee(e))
      .catch((err) => active && setLoadError(getErrorMessage(err)))
    return () => {
      active = false
    }
  }, [id])

  if (loadError) return <p className="alert alert-error">{loadError}</p>
  if (!employee) return <p className="muted">Loading employee…</p>

  const rules = { currentDepartmentId: employee.departmentId, joiningDate: employee.joiningDate }
  const isInactive = employee.status === 'INACTIVE'
  const targetDepartment = departments.find((d) => d.id === Number(values.newDepartmentId))

  const handleChange = (event) => {
    const next = { ...values, [event.target.name]: event.target.value }
    setValues(next)
    if (errors[event.target.name]) {
      setErrors((prev) => ({ ...prev, [event.target.name]: validateTransfer(next, rules)[event.target.name] }))
    }
  }

  const handleSubmit = async (event) => {
    event.preventDefault()
    const validationErrors = validateTransfer(values, rules)
    setErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) return

    setSubmitting(true)
    try {
      const result = await transferEmployee(employee.id, {
        newDepartmentId: Number(values.newDepartmentId),
        transferDate: values.transferDate,
        reason: values.reason.trim(),
      })
      toast.success(
        `${employee.fullName} transferred to ${result.employee.departmentName}. ` +
          `Employee code changed ${result.transfer.oldEmployeeCode} → ${result.employee.employeeCode}`,
      )
      navigate(`/employees/${employee.id}`)
    } catch (err) {
      setErrors(getFieldErrors(err))
      toast.error(getErrorMessage(err))
      setSubmitting(false)
    }
  }

  return (
    <section>
      <Link to={`/employees/${employee.id}`} className="back-link">
        ← Back to {employee.fullName}
      </Link>
      <div className="page-header">
        <div>
          <h1>Employee Department Transfer</h1>
          <p className="muted">A new employee code is generated for the new department. The internal ID stays the same.</p>
        </div>
      </div>

      {isInactive && (
        <p className="alert alert-error" role="alert">
          This employee is inactive and cannot be transferred.
        </p>
      )}

      <form className="card form-card narrow" onSubmit={handleSubmit} noValidate>
        <div className="form-grid">
          <FormField label="Employee Name" name="employeeName">
            <input value={employee.fullName} readOnly disabled />
          </FormField>
          <FormField label="Current Employee Code" name="currentCode">
            <input value={employee.employeeCode} readOnly disabled />
          </FormField>
          <FormField label="Current Department" name="currentDepartment">
            <input value={employee.departmentName} readOnly disabled />
          </FormField>
          <FormField label="New Department" name="newDepartmentId" required error={errors.newDepartmentId}
            hint={targetDepartment ? `New code will be the next ${targetDepartment.codePrefix}xxxx number` : undefined}>
            <select value={values.newDepartmentId} onChange={handleChange} disabled={isInactive}>
              <option value="">Select new department</option>
              {departments
                .filter((d) => d.id !== employee.departmentId)
                .map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.name}
                  </option>
                ))}
            </select>
          </FormField>
          <FormField label="Transfer Date" name="transferDate" required error={errors.transferDate}>
            <input type="date" min={employee.joiningDate} max={todayIso()} value={values.transferDate}
              onChange={handleChange} disabled={isInactive} />
          </FormField>
          <FormField label="Transfer Reason" name="reason" required error={errors.reason}
            hint={`${values.reason.length}/500 characters`} className="span-2">
            <textarea rows={3} maxLength={500} value={values.reason} onChange={handleChange} disabled={isInactive} />
          </FormField>
        </div>
        <div className="form-actions">
          <Link to={`/employees/${employee.id}`} className="btn btn-ghost">
            Cancel
          </Link>
          <button type="submit" className="btn btn-primary" disabled={submitting || isInactive}>
            {submitting ? 'Transferring…' : 'Transfer employee'}
          </button>
        </div>
      </form>
    </section>
  )
}
