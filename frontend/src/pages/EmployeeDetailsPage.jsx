import { useCallback, useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { deactivateEmployee, getEmployee, getPersonalDetails, getTransferHistory } from '../api/employees'
import { useAuth } from '../auth/useAuth'
import StatusBadge from '../components/StatusBadge'
import { useToast } from '../components/toastContext'
import { GENDER_OPTIONS, formatCurrency, formatDate, labelOf } from '../utils/format'

function Detail({ label, children }) {
  return (
    <div className="detail">
      <dt>{label}</dt>
      <dd>{children ?? '—'}</dd>
    </div>
  )
}

/** Complete personal + employment details and the transfer history of one employee. */
export default function EmployeeDetailsPage() {
  const { id } = useParams()
  const { hasRole } = useAuth()
  const toast = useToast()
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  const load = useCallback(() => {
    let active = true
    Promise.all([getEmployee(id), getPersonalDetails(id), getTransferHistory(id)])
      .then(([employee, personal, transfers]) => {
        if (!active) return
        setData({ employee, personal, transfers })
        setError('')
      })
      .catch((err) => active && setError(getErrorMessage(err)))
    return () => {
      active = false
    }
  }, [id])

  useEffect(load, [load])

  if (error) return <p className="alert alert-error">{error}</p>
  if (!data) return <p className="muted">Loading employee…</p>

  const { employee, personal, transfers } = data
  const canManage = hasRole('ADMIN', 'MANAGER')
  const isActive = employee.status !== 'INACTIVE'

  const handleDeactivate = async () => {
    if (!window.confirm(`Deactivate ${employee.fullName} (${employee.employeeCode})? The record is kept.`)) return
    try {
      await deactivateEmployee(employee.id)
      toast.success(`${employee.fullName} has been deactivated`)
      load()
    } catch (err) {
      toast.error(getErrorMessage(err))
    }
  }

  return (
    <section>
      {canManage && (
        <Link to="/employees" className="back-link">
          ← Back to employees
        </Link>
      )}
      <div className="page-header">
        <div>
          <h1 className="with-badge">
            {employee.fullName} <StatusBadge status={employee.status} />
          </h1>
          <p className="muted">
            <span className="code-pill">{employee.employeeCode}</span> {employee.roleName} · {employee.departmentName}
          </p>
        </div>
        {canManage && (
          <div className="header-actions">
            <Link to={`/employees/${employee.id}/edit`} className="btn btn-ghost">
              Edit
            </Link>
            {isActive && (
              <Link to={`/employees/${employee.id}/transfer`} className="btn btn-ghost">
                Transfer
              </Link>
            )}
            {hasRole('ADMIN') && isActive && (
              <button type="button" className="btn btn-danger" onClick={handleDeactivate}>
                Deactivate
              </button>
            )}
          </div>
        )}
      </div>

      <div className="details-grid">
        <div className="card">
          <h2 className="section-title">Employment details</h2>
          <dl className="detail-list">
            <Detail label="Employee Code">{employee.employeeCode}</Detail>
            <Detail label="Department">{employee.departmentName}</Detail>
            <Detail label="Role / Designation">{employee.roleName}</Detail>
            <Detail label="Joining Date">{formatDate(employee.joiningDate)}</Detail>
            <Detail label="Annual Salary">{formatCurrency(employee.annualSalary)}</Detail>
            <Detail label="Status">
              <StatusBadge status={employee.status} />
            </Detail>
          </dl>
        </div>

        <div className="card">
          <h2 className="section-title">
            Personal details
            {personal.masked && <span className="masked-note">Aadhaar / PAN masked for your role</span>}
          </h2>
          <dl className="detail-list">
            <Detail label="Full Name">{employee.fullName}</Detail>
            <Detail label="Email">{employee.email}</Detail>
            <Detail label="Mobile">{employee.mobile}</Detail>
            <Detail label="Aadhaar Number">{personal.aadhaarNumber}</Detail>
            <Detail label="PAN Number">{personal.panNumber}</Detail>
            <Detail label="Date of Birth">{formatDate(personal.dateOfBirth)}</Detail>
            <Detail label="Gender">{labelOf(GENDER_OPTIONS, employee.gender)}</Detail>
            <Detail label="Address">{employee.address}</Detail>
          </dl>
        </div>
      </div>

      <div className="card">
        <h2 className="section-title">Department transfer history</h2>
        {transfers.length === 0 ? (
          <p className="muted">No department transfers yet.</p>
        ) : (
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Transfer Date</th>
                  <th>From</th>
                  <th>To</th>
                  <th>Old Code</th>
                  <th>New Code</th>
                  <th>Reason</th>
                  <th>By</th>
                </tr>
              </thead>
              <tbody>
                {transfers.map((t) => (
                  <tr key={t.id}>
                    <td className="nowrap">{formatDate(t.transferDate)}</td>
                    <td>{t.oldDepartmentName}</td>
                    <td>{t.newDepartmentName}</td>
                    <td><span className="code-pill code-pill-old">{t.oldEmployeeCode}</span></td>
                    <td><span className="code-pill">{t.newEmployeeCode}</span></td>
                    <td>{t.reason}</td>
                    <td>{t.transferredBy ?? 'System'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </section>
  )
}
